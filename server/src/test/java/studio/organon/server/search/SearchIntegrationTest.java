package studio.organon.server.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import studio.organon.server.domain.corpus.Epoch;
import studio.organon.server.domain.logic.FormalScheme;
import studio.organon.server.domain.logic.PremiseType;
import studio.organon.server.search.dto.SearchHitType;
import studio.organon.server.search.dto.SearchResultDto;
import studio.organon.server.service.ArgumentService;
import studio.organon.server.service.CorpusService;
import studio.organon.server.service.SemanticsService;
import studio.organon.server.web.dto.ArgumentRequest;
import studio.organon.server.web.dto.PassageRequest;
import studio.organon.server.web.dto.PhilosopherRequest;
import studio.organon.server.web.dto.PremiseRequest;
import studio.organon.server.web.dto.SemanticConceptRequest;
import studio.organon.server.web.dto.TermDefinitionRequest;
import studio.organon.server.web.dto.WorkRequest;

/**
 * La busqueda por palabras contra el PostgreSQL real: tildes, prefijos, pesos y
 * resaltado dependen de la configuracion es_organon, y eso solo se prueba ahi.
 *
 * <p>Cada prueba corre dentro de una transaccion que se deshace al terminar, asi
 * que el cuaderno de desarrollo queda como estaba. Por lo mismo el indexador no
 * llega a enterarse: solo avisa cuando una transaccion se confirma.
 */
@Tag("integracion")
@Transactional
@SpringBootTest(properties = {"organon.seed.enabled=false", "organon.search.reindex-on-startup=false"})
class SearchIntegrationTest {

    @Autowired
    private CorpusService corpus;

    @Autowired
    private SemanticsService semantics;

    @Autowired
    private ArgumentService arguments;

    @Autowired
    private SearchService search;

    private long autora;
    private long libro;

    @BeforeEach
    void cuaderno() {
        autora = corpus.createPhilosopher(
                new PhilosopherRequest("Pensadora de prueba de búsqueda", Epoch.MODERNA, null, null, null)).id();
        libro = corpus.createWork(new WorkRequest(autora, "Tratado de prueba", null, null, null, null)).id();
    }

    @Test
    @DisplayName("Sin tilde encuentra la palabra con tilde, y a medio escribir tambien")
    void sinTildesYConPrefijo() {
        long pasaje = corpus.createPassage(new PassageRequest(
                libro, "§1", "La razón no puede conocer lo que excede la experiencia.", null, null)).id();

        assertThat(search.text("razon", 20)).extracting(SearchResultDto::id).contains(pasaje);
        assertThat(search.text("razo", 20)).extracting(SearchResultDto::id).contains(pasaje);
    }

    @Test
    @DisplayName("Lo que dice el texto pesa mas que lo que dicen las notas")
    void elTextoPesaMasQueLasNotas() {
        long enNotas = corpus.createPassage(new PassageRequest(
                libro, "§2", "Un párrafo cualquiera sobre el método.", null, "Me recuerda a la melancolía de Burton.")).id();
        long enTexto = corpus.createPassage(new PassageRequest(
                libro, "§3", "La melancolía es una pasión del alma.", null, null)).id();

        List<Long> orden = search.text("melancolía", 20).stream()
                .filter(resultado -> resultado.type() == SearchHitType.PASSAGE)
                .map(SearchResultDto::id)
                .toList();

        assertThat(orden).containsSubsequence(enTexto, enNotas);
    }

    @Test
    @DisplayName("Una idea se encuentra por lo que dicen sus razones, con la coincidencia resaltada")
    void ideaPorSusRazones() {
        long idea = arguments.createArgument(new ArgumentRequest(libro, null, "Aquiles nunca alcanza",
                FormalScheme.NO_CLASIFICADO, null,
                List.of(new PremiseRequest(null, "La tortuga siempre lleva ventaja.", false, PremiseType.EMPIRICA))))
                .id();

        SearchResultDto resultado = search.text("tortuga", 20).stream()
                .filter(r -> r.type() == SearchHitType.ARGUMENT && r.id() == idea)
                .findFirst()
                .orElseThrow();

        assertThat(resultado.snippet()).contains(SearchService.MARCA_INICIO + "tortuga" + SearchService.MARCA_FIN);
        assertThat(resultado.workId()).isEqualTo(libro);
    }

    @Test
    @DisplayName("Una definicion se encuentra por su palabra y lleva a su concepto")
    void definicionPorSuPalabra() {
        long concepto = semantics.createConcept(new SemanticConceptRequest("Quintaesencia", null)).id();
        long definicion = semantics.createDefinition(new TermDefinitionRequest(
                concepto, autora, null, "Lo que queda cuando se quita todo lo accidental.", null)).id();

        SearchResultDto resultado = search.text("quintaesencia", 20).stream()
                .filter(r -> r.type() == SearchHitType.DEFINITION && r.id() == definicion)
                .findFirst()
                .orElseThrow();

        assertThat(resultado.conceptId()).isEqualTo(concepto);
        assertThat(resultado.author()).isEqualTo("Pensadora de prueba de búsqueda");
    }

    @Test
    @DisplayName("Sin letras no hay busqueda, y no es un error")
    void consultaSinLetras() {
        assertThat(search.text("¿?", 20)).isEmpty();
    }
}
