package studio.organon.server.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studio.organon.server.domain.corpus.Work;
import studio.organon.server.domain.logic.Argument;
import studio.organon.server.domain.logic.Objection;
import studio.organon.server.domain.logic.Premise;
import studio.organon.server.domain.logic.PremiseType;
import studio.organon.server.domain.semantics.TermDefinition;
import studio.organon.server.repository.ArgumentRepository;
import studio.organon.server.repository.PassageRepository;
import studio.organon.server.repository.TermDefinitionRepository;

/**
 * Sacar el trabajo fuera de la plataforma.
 *
 * <p>Markdown apunta a una boveda Zettelkasten: notas atomicas enlazadas con
 * wikilinks, para que cada argumento y cada termino sea un nodo propio en
 * Obsidian. LaTeX apunta al articulo: un bloque que se pega en el manuscrito y
 * compila sin dependencias exoticas.
 */
@Service
@Transactional(readOnly = true)
public class ExportService {

    private final ArgumentRepository argumentRepository;
    private final PassageRepository passageRepository;
    private final TermDefinitionRepository definitionRepository;
    private final CorpusService corpusService;
    private final ArgumentService argumentService;

    public ExportService(ArgumentRepository argumentRepository,
                         PassageRepository passageRepository,
                         TermDefinitionRepository definitionRepository,
                         CorpusService corpusService,
                         ArgumentService argumentService) {
        this.argumentRepository = argumentRepository;
        this.passageRepository = passageRepository;
        this.definitionRepository = definitionRepository;
        this.corpusService = corpusService;
        this.argumentService = argumentService;
    }

    // ----- Markdown / Zettelkasten ----------------------------------------

    /**
     * Devuelve la obra convertida en notas atomicas. Cada nota va precedida de
     * un marcador de fichero para que el usuario pueda partir el volcado en
     * archivos sueltos dentro de su boveda.
     */
    public String toMarkdown(Long workId) {
        Work work = corpusService.requireWork(workId);
        List<Argument> arguments = argumentRepository.findByWorkIdOrderByNameAsc(workId);
        List<TermDefinition> definitions =
                definitionRepository.findForReadingContext(work.getPhilosopher().getId(), workId);

        StringBuilder out = new StringBuilder();
        appendWorkNote(out, work, arguments, definitions);
        arguments.forEach(argument -> appendArgumentNote(out, argument));
        definitions.forEach(definition -> appendTermNote(out, definition));
        return out.toString();
    }

    private void appendWorkNote(StringBuilder out, Work work, List<Argument> arguments,
                                List<TermDefinition> definitions) {
        out.append("<!-- file: ").append(slug(work.getTitle())).append(".md -->\n");
        out.append("---\n");
        out.append("title: \"").append(work.getTitle()).append("\"\n");
        out.append("type: obra\n");
        out.append("autor: \"").append(work.getPhilosopher().getName()).append("\"\n");
        if (work.getOriginalYear() != null) {
            out.append("year: ").append(work.getOriginalYear()).append('\n');
        }
        out.append("tags: [organon, obra]\n");
        out.append("---\n\n");

        out.append("# ").append(work.getTitle()).append("\n\n");
        out.append("**Autor:** [[").append(work.getPhilosopher().getName()).append("]]\n\n");
        if (work.getPhilosophicalProblem() != null) {
            out.append("**Problema:** ").append(work.getPhilosophicalProblem()).append("\n\n");
        }
        if (work.getCoreThesis() != null) {
            out.append("**Tesis central:** ").append(work.getCoreThesis()).append("\n\n");
        }
        if (work.getDirectAdversary() != null) {
            out.append("**Escribe contra:** [[").append(slug(work.getDirectAdversary().getTitle()))
                    .append("|").append(work.getDirectAdversary().getTitle()).append("]]\n\n");
        }

        out.append("## Argumentos\n\n");
        if (arguments.isEmpty()) {
            out.append("_Sin argumentos reconstruidos todavia._\n\n");
        } else {
            arguments.forEach(argument -> out
                    .append("- [[argumento-").append(argument.getId()).append('|')
                    .append(argument.getName()).append("]] - `")
                    .append(argument.getFormalScheme()).append("` / ")
                    .append(argument.getSoundStatus()).append('\n'));
            out.append('\n');
        }

        out.append("## Terminos fijados\n\n");
        if (definitions.isEmpty()) {
            out.append("_Sin definiciones registradas._\n\n");
        } else {
            definitions.forEach(definition -> out
                    .append("- [[").append(definition.getConcept().getTerm()).append("]]\n"));
            out.append('\n');
        }

        out.append("## Pasajes\n\n");
        passageRepository.findByWorkIdOrderByLocatorAsc(work.getId()).forEach(passage -> out
                .append("### `").append(passage.getLocator()).append("`\n\n> ")
                .append(passage.getTextContent().replace("\n", "\n> ")).append("\n\n"));

        out.append("\n---\n\n");
    }

    private void appendArgumentNote(StringBuilder out, Argument argument) {
        out.append("<!-- file: argumento-").append(argument.getId()).append(".md -->\n");
        out.append("---\n");
        out.append("title: \"").append(argument.getName()).append("\"\n");
        out.append("type: argumento\n");
        out.append("esquema: ").append(argument.getFormalScheme()).append('\n');
        out.append("estado: ").append(argument.getSoundStatus()).append('\n');
        out.append("tags: [organon, argumento]\n");
        out.append("---\n\n");

        out.append("# ").append(argument.getName()).append("\n\n");
        out.append("**Obra:** [[").append(slug(argument.getWork().getTitle())).append('|')
                .append(argument.getWork().getTitle()).append("]]  \n");
        out.append("**Autor:** [[").append(argument.getWork().getPhilosopher().getName()).append("]]  \n");
        if (argument.getPassage() != null) {
            out.append("**Pasaje:** `").append(argument.getPassage().getLocator()).append("`  \n");
        }
        out.append("**Esquema:** `").append(argument.getFormalScheme()).append("`\n\n");

        out.append("## Forma estandar\n\n");
        List<Premise> ordered = orderedPremises(argument);
        int premiseNumber = 0;
        for (Premise premise : ordered) {
            boolean conclusion = premise.getPremiseType() == PremiseType.CONCLUSION;
            String tag = conclusion ? "**C**" : "**P" + (++premiseNumber) + "**";
            out.append("- ").append(tag).append(' ').append(premise.getStatement());
            out.append("  `").append(premise.getPremiseType()).append('`');
            if (premise.isEnthymeme()) {
                out.append(" _(entimema)_");
            }
            out.append('\n');
        }
        out.append('\n');

        if (hasText(argument.getLatexFormalization())) {
            out.append("## Formalizacion\n\n$$\n")
                    .append(argument.getLatexFormalization())
                    .append("\n$$\n\n");
        }

        List<Objection> objections = ordered.stream().flatMap(p -> p.getObjections().stream()).toList();
        out.append("## Objeciones\n\n");
        if (objections.isEmpty()) {
            out.append("_Sin objeciones registradas._\n\n");
        } else {
            objections.forEach(objection -> out
                    .append("- `").append(objection.getObjectionType()).append("` (P")
                    .append(objection.getPremise().getOrderIndex() + 1).append(") - ")
                    .append(objection.getExplanation()).append('\n'));
            out.append('\n');
        }

        out.append("\n---\n\n");
    }

    private void appendTermNote(StringBuilder out, TermDefinition definition) {
        String term = definition.getConcept().getTerm();
        out.append("<!-- file: ").append(slug(term)).append(".md -->\n");
        out.append("---\n");
        out.append("title: \"").append(term).append("\"\n");
        out.append("type: termino\n");
        out.append("autor: \"").append(definition.getPhilosopher().getName()).append("\"\n");
        out.append("tags: [organon, termino]\n");
        out.append("---\n\n");

        out.append("# ").append(term).append("\n\n");
        out.append("> [!warning] Termino sobrecargado\n");
        out.append("> Esta acepcion vale para [[").append(definition.getPhilosopher().getName())
                .append("]]");
        if (definition.getWork() != null) {
            out.append(" en [[").append(slug(definition.getWork().getTitle())).append('|')
                    .append(definition.getWork().getTitle()).append("]]");
        }
        out.append(". Otros autores usan la misma palabra en otro sentido.\n\n");

        out.append(definition.getOperationalDefinition()).append("\n\n");
        if (hasText(definition.getNotes())) {
            out.append("**Notas:** ").append(definition.getNotes()).append("\n\n");
        }
        out.append("\n---\n\n");
    }

    // ----- LaTeX academico ------------------------------------------------

    /**
     * Bloque LaTeX autocontenido: solo usa entornos de base (enumerate,
     * itemize, equation*), asi que compila en cualquier articulo sin cargar
     * paquetes adicionales mas alla de amsmath.
     */
    public String toLatex(Long argumentId) {
        Argument argument = argumentService.requireArgument(argumentId);
        List<Premise> ordered = orderedPremises(argument);

        StringBuilder out = new StringBuilder();
        out.append("% ---------------------------------------------------------------\n");
        out.append("% Organon Studio - exportacion LaTeX\n");
        out.append("% Requiere: \\usepackage{amsmath}\n");
        out.append("% ---------------------------------------------------------------\n\n");

        out.append("\\subsection*{").append(latexEscape(argument.getName())).append("}\n\n");

        out.append("\\noindent\\textit{Fuente:} ")
                .append(latexEscape(argument.getWork().getPhilosopher().getName()))
                .append(", \\emph{").append(latexEscape(argument.getWork().getTitle())).append('}');
        if (argument.getPassage() != null) {
            out.append(", ").append(latexEscape(argument.getPassage().getLocator()));
        }
        out.append(".\\\\\n");
        out.append("\\textit{Esquema:} \\texttt{")
                .append(latexEscape(argument.getFormalScheme().name())).append("} \\quad ");
        out.append("\\textit{Estado:} \\texttt{")
                .append(latexEscape(argument.getSoundStatus().name())).append("}\n\n");

        out.append("\\begin{enumerate}\n");
        int premiseNumber = 0;
        for (Premise premise : ordered) {
            boolean conclusion = premise.getPremiseType() == PremiseType.CONCLUSION;
            String label = conclusion ? "C" : "P$_{" + (++premiseNumber) + "}$";
            out.append("  \\item[").append(label).append("] ")
                    .append(latexEscape(premise.getStatement()));
            if (premise.isEnthymeme()) {
                // El entimema se marca en el propio texto: quien lea el articulo
                // debe saber que ese enunciado lo repone el interprete.
                out.append(" \\hfill \\textsc{(entimema)}");
            }
            out.append('\n');
        }
        out.append("\\end{enumerate}\n\n");

        if (hasText(argument.getLatexFormalization())) {
            out.append("\\begin{equation*}\n  ")
                    .append(argument.getLatexFormalization().trim())
                    .append("\n\\end{equation*}\n\n");
        }

        List<Objection> objections = ordered.stream().flatMap(p -> p.getObjections().stream()).toList();
        if (!objections.isEmpty()) {
            out.append("\\paragraph{Objeciones.}\n\\begin{itemize}\n");
            for (Objection objection : objections) {
                out.append("  \\item \\textbf{")
                        .append(latexEscape(humanize(objection.getObjectionType().name())))
                        .append("} (P$_{").append(objection.getPremise().getOrderIndex() + 1)
                        .append("}$): ").append(latexEscape(objection.getExplanation())).append('\n');
            }
            out.append("\\end{itemize}\n");
        }

        return out.toString();
    }

    // ----- Utilidades -----------------------------------------------------

    /** Neutraliza los caracteres que LaTeX interpreta como sintaxis. */
    static String latexEscape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (char c : value.toCharArray()) {
            switch (c) {
                case '\\' -> escaped.append("\\textbackslash{}");
                case '&', '%', '$', '#', '_', '{', '}' -> escaped.append('\\').append(c);
                case '~' -> escaped.append("\\textasciitilde{}");
                case '^' -> escaped.append("\\textasciicircum{}");
                default -> escaped.append(c);
            }
        }
        return escaped.toString();
    }

    /** Nombre de fichero apto para una boveda de Obsidian. */
    private static String slug(String value) {
        String normalized = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private static List<Premise> orderedPremises(Argument argument) {
        return argument.getPremises().stream()
                .sorted((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()))
                .toList();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String humanize(String enumName) {
        String lower = enumName.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
