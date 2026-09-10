package studio.organon.server.bootstrap;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import studio.organon.server.domain.corpus.Epoch;
import studio.organon.server.domain.corpus.Passage;
import studio.organon.server.domain.corpus.Philosopher;
import studio.organon.server.domain.corpus.Work;
import studio.organon.server.domain.dialectic.DialecticalRelation;
import studio.organon.server.domain.dialectic.RelationType;
import studio.organon.server.domain.logic.Argument;
import studio.organon.server.domain.logic.FormalScheme;
import studio.organon.server.domain.logic.Objection;
import studio.organon.server.domain.logic.ObjectionType;
import studio.organon.server.domain.logic.Premise;
import studio.organon.server.domain.logic.PremiseType;
import studio.organon.server.domain.logic.SoundStatus;
import studio.organon.server.domain.semantics.SemanticConcept;
import studio.organon.server.domain.semantics.TermDefinition;
import studio.organon.server.repository.ArgumentRepository;
import studio.organon.server.repository.DialecticalRelationRepository;
import studio.organon.server.repository.PassageRepository;
import studio.organon.server.repository.PhilosopherRepository;
import studio.organon.server.repository.SemanticConceptRepository;
import studio.organon.server.repository.TermDefinitionRepository;
import studio.organon.server.repository.WorkRepository;

/**
 * Carga el corpus de arranque: el debate moderno sobre causalidad y sustancia.
 *
 * <p>No son datos de relleno. Los cuatro autores forman una cadena real de
 * discusión — Descartes deduce a Dios de la idea de infinito, Spinoza radicaliza
 * su propia definición de sustancia hasta el monismo, Hume demuele la conexión
 * necesaria que ambos presuponían y Kant responde convirtiendo la causalidad en
 * condición de posibilidad de la experiencia — y usan las mismas tres palabras
 * («sustancia», «idea», «causa») en sentidos incompatibles. Eso es justo lo que
 * la plataforma existe para hacer visible.
 *
 * <p>Es idempotente: si ya hay filósofos en la base, no toca nada.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final PhilosopherRepository philosopherRepository;
    private final WorkRepository workRepository;
    private final PassageRepository passageRepository;
    private final SemanticConceptRepository conceptRepository;
    private final TermDefinitionRepository definitionRepository;
    private final ArgumentRepository argumentRepository;
    private final DialecticalRelationRepository relationRepository;
    private final boolean enabled;

    public DataInitializer(PhilosopherRepository philosopherRepository,
                           WorkRepository workRepository,
                           PassageRepository passageRepository,
                           SemanticConceptRepository conceptRepository,
                           TermDefinitionRepository definitionRepository,
                           ArgumentRepository argumentRepository,
                           DialecticalRelationRepository relationRepository,
                           @Value("${organon.seed.enabled:true}") boolean enabled) {
        this.philosopherRepository = philosopherRepository;
        this.workRepository = workRepository;
        this.passageRepository = passageRepository;
        this.conceptRepository = conceptRepository;
        this.definitionRepository = definitionRepository;
        this.argumentRepository = argumentRepository;
        this.relationRepository = relationRepository;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Semilla desactivada (organon.seed.enabled=false)");
            return;
        }
        if (philosopherRepository.count() > 0) {
            log.info("El corpus ya tiene datos; la semilla no hace nada");
            return;
        }

        log.info("Cargando semilla: debate moderno sobre causalidad y sustancia");

        Philosopher descartes = philosopherRepository.save(new Philosopher(
                "René Descartes", Epoch.MODERNA, "Racionalismo",
                "Funda la filosofía moderna sobre la certeza del sujeto pensante. La duda "
                        + "metódica arrasa con todo saber recibido para dejar en pie un único punto "
                        + "de apoyo, el cogito, desde el que intenta reconstruir la física y la "
                        + "metafísica con el rigor de la geometría."));

        Philosopher spinoza = philosopherRepository.save(new Philosopher(
                "Baruch Spinoza", Epoch.MODERNA, "Racionalismo",
                "Toma la definición cartesiana de sustancia y la aplica sin concesiones: si "
                        + "sustancia es lo que es en sí y se concibe por sí, solo puede haber una. "
                        + "El dualismo de Descartes se disuelve en un monismo donde pensamiento y "
                        + "extensión son atributos de lo mismo."));

        Philosopher hume = philosopherRepository.save(new Philosopher(
                "David Hume", Epoch.MODERNA, "Empirismo",
                "Lleva el empirismo hasta sus consecuencias escépticas. Si toda idea procede "
                        + "de una impresión, y no hay impresión de conexión necesaria, entonces la "
                        + "causalidad no es un descubrimiento de la razón sino una costumbre de la "
                        + "imaginación."));

        Philosopher kant = philosopherRepository.save(new Philosopher(
                "Immanuel Kant", Epoch.MODERNA, "Idealismo trascendental",
                "Reconoce que Hume tiene razón en que la necesidad causal no se lee en la "
                        + "experiencia, y concluye lo contrario que él: la causalidad no viene "
                        + "después de la experiencia porque es una de sus condiciones de "
                        + "posibilidad."));

        Work meditaciones = workRepository.save(new Work(
                descartes, "Meditaciones metafísicas", 1641,
                "¿Queda algo en pie tras la duda universal, y basta para recuperar el mundo?",
                "El cogito resiste a la duda, y de la idea de infinito que encuentro en mí se "
                        + "sigue la existencia de Dios, garante último de la verdad de lo claro y distinto."));

        Work etica = workRepository.save(new Work(
                spinoza, "Ética demostrada según el orden geométrico", 1677,
                "¿Cuántas sustancias hay, si se toma en serio lo que «sustancia» significa?",
                "Solo existe una sustancia infinita, Dios o Naturaleza, de la que todo lo demás "
                        + "es modo. El dualismo es un error de análisis conceptual."));

        Work tratado = workRepository.save(new Work(
                hume, "Tratado de la naturaleza humana", 1739,
                "¿Qué garantiza que el futuro se parecerá al pasado?",
                "Nada en la impresión de dos sucesos contiene su conexión necesaria. La "
                        + "causalidad es hábito, no inferencia demostrativa."));

        Work critica = workRepository.save(new Work(
                kant, "Crítica de la razón pura", 1781,
                "¿Cómo son posibles los juicios sintéticos a priori?",
                "El entendimiento no extrae sus leyes de la naturaleza: se las prescribe. La "
                        + "causalidad es categoría, condición de que haya experiencia objetiva."));

        // La polémica declarada a nivel de obra, previa a las relaciones finas
        // entre argumentos concretos.
        etica.setDirectAdversary(meditaciones);
        tratado.setDirectAdversary(meditaciones);
        critica.setDirectAdversary(tratado);

        seedPassages(meditaciones, etica, tratado, critica);
        seedGlossary(descartes, spinoza, hume, kant, meditaciones, etica, tratado, critica);
        seedArguments(meditaciones, etica, tratado, critica);

        log.info("Semilla cargada: {} filósofos, {} obras, {} pasajes, {} argumentos, {} relaciones",
                philosopherRepository.count(), workRepository.count(), passageRepository.count(),
                argumentRepository.count(), relationRepository.count());
    }

    // -----------------------------------------------------------------------
    // Pasajes, con los locators canónicos que usa la tradición académica
    // -----------------------------------------------------------------------

    private void seedPassages(Work meditaciones, Work etica, Work tratado, Work critica) {
        passageRepository.saveAll(List.of(
                new Passage(meditaciones, "AT VII 40",
                        "Ahora bien, es manifiesto por la luz natural que debe haber al menos "
                                + "tanta realidad en la causa eficiente y total como en el efecto de "
                                + "esa misma causa. Pues, ¿de dónde puede el efecto sacar su realidad, "
                                + "sino de la causa? ¿Y cómo podría esa causa dársela, si no la "
                                + "tuviera ella misma?",
                        40),
                new Passage(meditaciones, "AT VII 45",
                        "Por el nombre de Dios entiendo una sustancia infinita, eterna, inmutable, "
                                + "independiente, omnisciente, omnipotente, y por la cual yo mismo y "
                                + "todas las demás cosas que existen hemos sido creados. Ahora bien, "
                                + "estas cosas son tales que, cuanto más atentamente las considero, "
                                + "tanto menos me parece que puedan proceder de mí solo.",
                        45),
                new Passage(etica, "Ética I, Def. III",
                        "Por sustancia entiendo aquello que es en sí y se concibe por sí, esto es, "
                                + "aquello cuyo concepto no necesita del concepto de otra cosa a "
                                + "partir del cual deba ser formado.",
                        null),
                new Passage(etica, "Ética I, Prop. XIV",
                        "Salvo Dios, no puede darse ni concebirse sustancia alguna. En efecto, "
                                + "siendo Dios el ser absolutamente infinito, del que no puede "
                                + "negarse ningún atributo que exprese esencia de sustancia, y "
                                + "existiendo necesariamente, si hubiera alguna sustancia además de "
                                + "Dios, debería explicarse por algún atributo de Dios, y así "
                                + "existirían dos sustancias con el mismo atributo, lo cual es absurdo.",
                        null),
                new Passage(tratado, "THN 1.3.2",
                        "Al considerar la relación de causa y efecto, no encuentro cualidad alguna "
                                + "que descubra el lazo entre ambos, ni nada por lo que pueda inferir "
                                + "con certeza la existencia del uno a partir del otro. Todo lo que "
                                + "observo es que uno sigue realmente al otro; el impulso de la "
                                + "primera bola va acompañado del movimiento de la segunda. Esto es "
                                + "todo lo que aparece a los sentidos externos.",
                        null),
                new Passage(tratado, "THN 1.3.14",
                        "No teniendo impresión alguna de conexión necesaria, no podemos tener idea "
                                + "de ella. La necesidad es algo que existe en la mente, no en los "
                                + "objetos; y jamás formamos la más remota idea de ella considerada "
                                + "como cualidad de los cuerpos.",
                        null),
                new Passage(critica, "KrV B 4",
                        "La necesidad y la estricta universalidad son, pues, señales seguras de un "
                                + "conocimiento a priori, y van inseparablemente unidas. La "
                                + "experiencia nunca da a sus juicios universalidad verdadera y "
                                + "estricta, sino solo supuesta y comparativa, mediante inducción.",
                        null),
                new Passage(critica, "KrV B 232",
                        "Todos los cambios suceden según la ley del enlace de causa y efecto. La "
                                + "aprehensión de lo múltiple del fenómeno es siempre sucesiva; pero "
                                + "que la sucesión en la aprehensión corresponda a una sucesión "
                                + "objetiva no puede saberse sino en tanto se supone una regla que "
                                + "determina el orden.",
                        null)));
    }

    // -----------------------------------------------------------------------
    // Glosario: las mismas tres palabras en sentidos incompatibles
    // -----------------------------------------------------------------------

    private void seedGlossary(Philosopher descartes, Philosopher spinoza, Philosopher hume,
                              Philosopher kant, Work meditaciones, Work etica, Work tratado,
                              Work critica) {
        SemanticConcept sustancia = conceptRepository.save(new SemanticConcept("Sustancia",
                "Aquello que subsiste por sí. El desacuerdo sobre cuántas cosas cumplen esa "
                        + "condición divide toda la metafísica moderna."));
        SemanticConcept idea = conceptRepository.save(new SemanticConcept("Idea",
                "Contenido mental. Si la idea es imagen de una impresión o representación de una "
                        + "esencia cambia por completo lo que puede probarse a partir de ella."));
        SemanticConcept causa = conceptRepository.save(new SemanticConcept("Causa",
                "Aquello de lo que algo se sigue. El pleito moderno es si la conexión causal se "
                        + "conoce, se supone o se impone."));

        definitionRepository.saveAll(List.of(
                // --- Sustancia ---
                new TermDefinition(sustancia, descartes, meditaciones,
                        "Cosa que existe de tal manera que no necesita de ninguna otra para existir. "
                                + "Admite grados: en sentido estricto solo Dios lo es, pero llama también "
                                + "sustancias a la pensante y la extensa, que solo necesitan del concurso divino.",
                        "La cláusula «salvo el concurso de Dios» es la grieta por la que entra Spinoza."),
                new TermDefinition(sustancia, spinoza, etica,
                        "Aquello que es en sí y se concibe por sí: su concepto no necesita del concepto "
                                + "de ninguna otra cosa. Sin grados y sin excepciones, de donde se sigue que "
                                + "solo hay una.",
                        "Es literalmente la definición de Descartes aplicada sin la salvedad que él se reservaba."),
                new TermDefinition(sustancia, hume, tratado,
                        "Colección de cualidades particulares unidas por la imaginación y designadas con "
                                + "un nombre. No hay impresión de un sustrato distinto de sus cualidades, "
                                + "luego tampoco idea legítima de él.",
                        "La disolución empirista: la sustancia no es refutada, es declarada sin contenido."),
                new TermDefinition(sustancia, kant, critica,
                        "Categoría del entendimiento: la regla que hace pensar lo permanente en el tiempo "
                                + "frente a lo que cambia. No es un objeto que se descubra, sino una condición "
                                + "bajo la cual algo puede darse como objeto.",
                        "Ni cosa (Descartes) ni ficción (Hume): función."),

                // --- Idea ---
                new TermDefinition(idea, descartes, meditaciones,
                        "Aquello que es inmediatamente percibido por el espíritu. Tiene realidad objetiva "
                                + "proporcional a la perfección de lo representado, y esa realidad exige causa.",
                        "El paso decisivo de la tercera meditación: la idea, en cuanto representación, tiene peso ontológico."),
                new TermDefinition(idea, spinoza, etica,
                        "Concepto que el alma forma por ser cosa pensante; no una pintura muda sobre un "
                                + "cuadro, sino un modo del atributo pensamiento, activo y con orden propio.",
                        "Contra la pasividad representacional: la idea verdadera se explica por su causa, no por su parecido."),
                new TermDefinition(idea, hume, tratado,
                        "Copia débil de una impresión previa. Todo el contenido de una idea procede de la "
                                + "impresión de la que deriva; sin impresión de origen no hay idea, solo palabra.",
                        "Este criterio es el arma con que despacha sustancia, yo y conexión necesaria."),
                new TermDefinition(idea, kant, critica,
                        "Concepto de la razón al que no puede corresponder ningún objeto de la experiencia "
                                + "(alma, mundo, Dios). Regula la investigación, pero no constituye conocimiento.",
                        "Kant reserva «idea» para lo que Descartes creía poder probar."),

                // --- Causa ---
                new TermDefinition(causa, descartes, meditaciones,
                        "Aquello que comunica realidad a su efecto. Principio evidente por luz natural: la "
                                + "causa contiene al menos tanta realidad, formal o eminente, como el efecto.",
                        "Es el motor de la prueba de Dios: sin este principio la tercera meditación no arranca."),
                new TermDefinition(causa, spinoza, etica,
                        "Razón de ser. Causa y razón coinciden: de la naturaleza de Dios se sigue todo con la "
                                + "misma necesidad con que de la del triángulo se siguen sus propiedades.",
                        "No hay contingencia ni voluntad divina: la causalidad es implicación lógica."),
                new TermDefinition(causa, hume, tratado,
                        "Objeto seguido de otro, donde todos los objetos semejantes al primero van seguidos "
                                + "de objetos semejantes al segundo. Conjunción constante más transición "
                                + "habitual de la mente; nada más.",
                        "La necesidad se traslada del objeto al observador."),
                new TermDefinition(causa, kant, critica,
                        "Categoría a priori del entendimiento que determina la sucesión objetiva de los "
                                + "fenómenos en el tiempo. No se extrae de la experiencia porque es lo que "
                                + "hace posible que haya experiencia de sucesos y no mero desfile de percepciones.",
                        "La respuesta a Hume: la necesidad no está en las cosas ni es un hábito, es la forma de la objetividad.")));
    }

    // -----------------------------------------------------------------------
    // Argumentos formalizados y la red que los enlaza
    // -----------------------------------------------------------------------

    private void seedArguments(Work meditaciones, Work etica, Work tratado, Work critica) {
        Argument cartesiano = buildCartesianProof(meditaciones);
        Argument spinozista = buildSpinozaMonism(etica);
        Argument humeano = buildHumeCritique(tratado);
        Argument kantiano = buildKantianDeduction(critica);

        argumentRepository.saveAll(List.of(cartesiano, spinozista, humeano, kantiano));

        relationRepository.saveAll(List.of(
                new DialecticalRelation(spinozista, cartesiano, RelationType.RADICALIZA,
                        "Spinoza no discute la definición cartesiana de sustancia: la toma al pie de la "
                                + "letra y elimina la salvedad con que Descartes salvaba el dualismo. El "
                                + "monismo es el cartesianismo llevado hasta el final."),
                new DialecticalRelation(humeano, cartesiano, RelationType.REFUTA,
                        "Si toda idea exige impresión de origen y no hay impresión de conexión necesaria, "
                                + "el principio causal del que pende toda la prueba deja de ser evidente por "
                                + "luz natural y pasa a ser un hábito sin fuerza demostrativa."),
                new DialecticalRelation(humeano, spinozista, RelationType.REFUTA,
                        "El monismo descansa en identificar causa con razón lógica. Sin conexión necesaria "
                                + "cognoscible, esa identificación queda sin apoyo."),
                new DialecticalRelation(kantiano, humeano, RelationType.PRESUPONE,
                        "Kant concede la premisa mayor de Hume: la necesidad causal no se lee en ninguna "
                                + "impresión. Es el punto de partida, no el adversario."),
                new DialecticalRelation(kantiano, humeano, RelationType.REFUTA,
                        "De esa misma premisa Kant extrae la conclusión contraria: si la necesidad no viene "
                                + "de la experiencia y sin embargo la experiencia objetiva la exige, entonces "
                                + "la aporta el entendimiento a priori.")));
    }

    private Argument buildCartesianProof(Work meditaciones) {
        Argument argument = new Argument(
                meditaciones,
                passage(meditaciones, "AT VII 40"),
                "Prueba de Dios por la realidad objetiva de la idea de infinito",
                FormalScheme.MODUS_PONENS,
                "\\big(\\forall e\\, \\forall c\\; \\mathrm{Caus}(c,e) \\to R(c) \\geq R(e)\\big) "
                        + "\\;\\wedge\\; \\mathrm{Ob}(i_{\\infty}) > R(\\mathrm{ego}) "
                        + "\\;\\vdash\\; \\exists D\\; \\mathrm{Caus}(D, i_{\\infty})");
        argument.setSoundStatus(SoundStatus.PENDIENTE);

        argument.addPremise(new Premise(0,
                "Debe haber al menos tanta realidad en la causa eficiente y total como en su efecto.",
                false, PremiseType.AXIOMATICA));
        argument.addPremise(new Premise(1,
                "Encuentro en mí la idea de una sustancia infinita, eterna e independiente.",
                false, PremiseType.EMPIRICA));

        Premise implicito = new Premise(2,
                "La realidad objetiva de una idea — el grado de ser de lo que representa — exige "
                        + "una causa que posea ese mismo grado de realidad formalmente.",
                true, PremiseType.AXIOMATICA);
        implicito.addObjection(new Objection(ObjectionType.PETICION_DE_PRINCIPIO,
                "Trata la representación como si tuviera grados de ser propios, cuando eso es "
                        + "precisamente lo que habría que demostrar. Sin esa asimilación entre "
                        + "representar lo infinito y contener realidad infinita, el principio causal "
                        + "no se aplica a las ideas y la prueba no arranca."));
        argument.addPremise(implicito);

        argument.addPremise(new Premise(3,
                "Yo, sustancia finita, no contengo realidad suficiente para ser causa de la "
                        + "realidad objetiva de lo infinito.",
                false, PremiseType.INFERENCIA_INTERMEDIA));
        argument.addPremise(new Premise(4,
                "Luego existe un ser infinito que ha puesto en mí esa idea.",
                false, PremiseType.CONCLUSION));
        return argument;
    }

    private Argument buildSpinozaMonism(Work etica) {
        Argument argument = new Argument(
                etica,
                passage(etica, "Ética I, Def. III"),
                "Unicidad de la sustancia",
                FormalScheme.REDUCTIO_AD_ABSURDUM,
                "\\neg \\exists s_1 s_2 \\big(\\mathrm{Sub}(s_1) \\wedge \\mathrm{Sub}(s_2) "
                        + "\\wedge s_1 \\neq s_2\\big) \\;\\therefore\\; \\exists! s\\; \\mathrm{Sub}(s)");
        argument.setSoundStatus(SoundStatus.PENDIENTE);

        argument.addPremise(new Premise(0,
                "Por sustancia se entiende aquello que es en sí y se concibe por sí, sin necesitar "
                        + "el concepto de ninguna otra cosa.",
                false, PremiseType.DEFINICION));
        argument.addPremise(new Premise(1,
                "Dos sustancias que tuvieran atributos distintos nada tendrían en común entre sí.",
                false, PremiseType.AXIOMATICA));

        Premise implicito = new Premise(2,
                "Lo que nada tiene en común con otra cosa no puede ser causa de ella ni ser "
                        + "concebido por medio de ella.",
                true, PremiseType.AXIOMATICA);
        implicito.addObjection(new Objection(ObjectionType.CONTRAEJEMPLO,
                "El principio se toma del axioma cartesiano de causalidad, pero exige además que "
                        + "toda relación de dependencia sea conceptual. Quien admita causas que no "
                        + "sean razones — una acción libre, por ejemplo — bloquea el paso."));
        argument.addPremise(implicito);

        argument.addPremise(new Premise(3,
                "Supóngase que existen dos sustancias: o comparten atributo, y entonces son "
                        + "indiscernibles, o no lo comparten, y entonces ninguna puede explicar a la otra.",
                false, PremiseType.INFERENCIA_INTERMEDIA));
        argument.addPremise(new Premise(4,
                "Luego no puede darse ni concebirse más que una sustancia: Dios o Naturaleza.",
                false, PremiseType.CONCLUSION));
        return argument;
    }

    private Argument buildHumeCritique(Work tratado) {
        Argument argument = new Argument(
                tratado,
                passage(tratado, "THN 1.3.2"),
                "Crítica de la conexión necesaria",
                FormalScheme.INDUCTIVE,
                "\\forall i\\, \\big(\\mathrm{Idea}(i) \\to \\exists p\\; \\mathrm{Impr}(p) "
                        + "\\wedge \\mathrm{Copia}(i,p)\\big) \\;\\wedge\\; "
                        + "\\neg\\exists p\\; \\mathrm{Impr}(p) \\wedge \\mathrm{Rep}\\big(p, \\Box(C \\to E)\\big) "
                        + "\\;\\vdash\\; \\neg\\, \\mathrm{Idea}\\big(\\Box(C \\to E)\\big)");
        argument.setSoundStatus(SoundStatus.PENDIENTE);

        argument.addPremise(new Premise(0,
                "Toda idea simple procede de una impresión previa de la que es copia.",
                false, PremiseType.AXIOMATICA));
        argument.addPremise(new Premise(1,
                "Al examinar dos sucesos enlazados solo se observa contigüidad, prioridad temporal "
                        + "y conjunción constante; ninguna impresión de la conexión misma.",
                false, PremiseType.EMPIRICA));

        Premise implicito = new Premise(2,
                "Un término sin impresión de origen carece de contenido representativo legítimo, "
                        + "por muy familiar que resulte su uso.",
                true, PremiseType.AXIOMATICA);
        implicito.addObjection(new Objection(ObjectionType.CONTRAEJEMPLO,
                "El propio criterio no procede de ninguna impresión: es una tesis sobre el origen "
                        + "de las ideas, no una idea copiada de un dato sensible. Aplicado a sí mismo, "
                        + "se elimina."));
        argument.addPremise(implicito);

        argument.addPremise(new Premise(3,
                "La necesidad que atribuimos al enlace procede de la transición habitual que la "
                        + "mente hace tras observar la conjunción repetida.",
                false, PremiseType.INFERENCIA_INTERMEDIA));
        argument.addPremise(new Premise(4,
                "Luego la conexión necesaria no está en los objetos: es una determinación del "
                        + "hábito de quien los observa.",
                false, PremiseType.CONCLUSION));
        return argument;
    }

    private Argument buildKantianDeduction(Work critica) {
        Argument argument = new Argument(
                critica,
                passage(critica, "KrV B 232"),
                "La causalidad como condición de posibilidad de la experiencia",
                FormalScheme.TRASCENDENTAL,
                "\\mathrm{Exp}_{\\mathrm{obj}} \\to \\Box\\,\\mathrm{Reg}(t) "
                        + "\\;\\wedge\\; \\mathrm{Exp}_{\\mathrm{obj}} "
                        + "\\;\\vdash\\; \\mathrm{Caus} \\in \\mathcal{A}_{\\text{a priori}}");
        argument.setSoundStatus(SoundStatus.PENDIENTE);

        argument.addPremise(new Premise(0,
                "Tenemos experiencia de sucesos objetivamente ordenados: distinguimos que el barco "
                        + "descendió río abajo de que meramente lo miramos en ese orden.",
                false, PremiseType.EMPIRICA));
        argument.addPremise(new Premise(1,
                "La aprehensión de lo múltiple es siempre sucesiva, de modo que el mero orden de "
                        + "las percepciones no basta para distinguir la sucesión objetiva de la subjetiva.",
                false, PremiseType.EMPIRICA));

        Premise implicito = new Premise(2,
                "Si una distinción es efectiva y no puede fundarse en el material dado, ha de "
                        + "fundarse en una regla que el sujeto aporta con anterioridad a lo dado.",
                true, PremiseType.AXIOMATICA);
        implicito.addObjection(new Objection(ObjectionType.FALSA_DICOTOMIA,
                "Supone que las únicas fuentes posibles son el dato sensible o una regla a priori "
                        + "del sujeto. Deja fuera una tercera vía — que la regularidad sea una "
                        + "propiedad del mundo aprendida falible y revisablemente — que es justo la "
                        + "que exploraría un naturalista posterior."));
        argument.addPremise(implicito);

        argument.addPremise(new Premise(3,
                "Esa regla es la del enlace de causa y efecto, que determina cuál de dos estados "
                        + "precede necesariamente al otro.",
                false, PremiseType.INFERENCIA_INTERMEDIA));
        argument.addPremise(new Premise(4,
                "Luego la causalidad no es una generalización extraída de la experiencia, sino una "
                        + "condición a priori de que haya experiencia objetiva.",
                false, PremiseType.CONCLUSION));
        return argument;
    }

    private Passage passage(Work work, String locator) {
        return passageRepository.findByWorkIdAndLocator(work.getId(), locator).orElse(null);
    }
}
