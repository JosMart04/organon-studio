package studio.organon.server.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studio.organon.server.domain.corpus.Epoch;
import studio.organon.server.domain.corpus.Passage;
import studio.organon.server.domain.corpus.Philosopher;
import studio.organon.server.domain.corpus.Work;
import studio.organon.server.error.ConflictException;
import studio.organon.server.error.NotFoundException;
import studio.organon.server.repository.PassageRepository;
import studio.organon.server.repository.PhilosopherRepository;
import studio.organon.server.repository.WorkRepository;
import studio.organon.server.web.dto.PassageDto;
import studio.organon.server.web.dto.PassageRequest;
import studio.organon.server.web.dto.PhilosopherDto;
import studio.organon.server.web.dto.PhilosopherRequest;
import studio.organon.server.web.dto.WorkDto;
import studio.organon.server.web.dto.WorkRequest;

/** Alta y consulta del corpus: quien escribio que, y que dice cada fragmento. */
@Service
@Transactional(readOnly = true)
public class CorpusService {

    private final PhilosopherRepository philosopherRepository;
    private final WorkRepository workRepository;
    private final PassageRepository passageRepository;
    private final DomainMapper mapper;

    public CorpusService(PhilosopherRepository philosopherRepository,
                         WorkRepository workRepository,
                         PassageRepository passageRepository,
                         DomainMapper mapper) {
        this.philosopherRepository = philosopherRepository;
        this.workRepository = workRepository;
        this.passageRepository = passageRepository;
        this.mapper = mapper;
    }

    // ----- Filosofos ------------------------------------------------------

    public List<PhilosopherDto> listPhilosophers(Epoch epoch) {
        List<Philosopher> found = epoch == null
                ? philosopherRepository.findAllByOrderByNameAsc()
                : philosopherRepository.findByEpochOrderByNameAsc(epoch);
        return found.stream().map(mapper::toDto).toList();
    }

    public PhilosopherDto getPhilosopher(Long id) {
        return mapper.toDto(requirePhilosopher(id));
    }

    @Transactional
    public PhilosopherDto createPhilosopher(PhilosopherRequest request) {
        philosopherRepository.findByNameIgnoreCase(request.name()).ifPresent(existing -> {
            throw new ConflictException("Ya existe un filosofo llamado " + existing.getName());
        });
        Philosopher philosopher = new Philosopher(
                request.name(), request.epoch(), request.school(), request.biographicalSummary());
        return mapper.toDto(philosopherRepository.save(philosopher));
    }

    @Transactional
    public PhilosopherDto updatePhilosopher(Long id, PhilosopherRequest request) {
        Philosopher philosopher = requirePhilosopher(id);
        philosopher.setName(request.name());
        philosopher.setEpoch(request.epoch());
        philosopher.setSchool(request.school());
        philosopher.setBiographicalSummary(request.biographicalSummary());
        return mapper.toDto(philosopher);
    }

    @Transactional
    public void deletePhilosopher(Long id) {
        philosopherRepository.delete(requirePhilosopher(id));
    }

    // ----- Obras ----------------------------------------------------------

    public List<WorkDto> listWorks(Long philosopherId) {
        List<Work> found = philosopherId == null
                ? workRepository.findAllByOrderByOriginalYearAscTitleAsc()
                : workRepository.findByPhilosopherIdOrderByOriginalYearAsc(philosopherId);
        return found.stream().map(mapper::toDto).toList();
    }

    public WorkDto getWork(Long id) {
        return mapper.toDto(requireWork(id));
    }

    @Transactional
    public WorkDto createWork(WorkRequest request) {
        Work work = new Work(
                requirePhilosopher(request.philosopherId()),
                request.title(),
                request.originalYear(),
                request.philosophicalProblem(),
                request.coreThesis());
        applyAdversary(work, request.directAdversaryId());
        return mapper.toDto(workRepository.save(work));
    }

    @Transactional
    public WorkDto updateWork(Long id, WorkRequest request) {
        Work work = requireWork(id);
        work.setPhilosopher(requirePhilosopher(request.philosopherId()));
        work.setTitle(request.title());
        work.setOriginalYear(request.originalYear());
        work.setPhilosophicalProblem(request.philosophicalProblem());
        work.setCoreThesis(request.coreThesis());
        applyAdversary(work, request.directAdversaryId());
        return mapper.toDto(work);
    }

    @Transactional
    public void deleteWork(Long id) {
        workRepository.delete(requireWork(id));
    }

    // ----- Pasajes --------------------------------------------------------

    public List<PassageDto> listPassages(Long workId) {
        requireWork(workId);
        return passageRepository.findByWorkIdOrderByLocatorAsc(workId).stream().map(mapper::toDto).toList();
    }

    public PassageDto getPassage(Long id) {
        return mapper.toDto(requirePassage(id));
    }

    @Transactional
    public PassageDto createPassage(PassageRequest request) {
        Passage passage = new Passage(
                requireWork(request.workId()),
                request.locator(),
                request.textContent(),
                request.pageNumber());
        return mapper.toDto(passageRepository.save(passage));
    }

    @Transactional
    public PassageDto updatePassage(Long id, PassageRequest request) {
        Passage passage = requirePassage(id);
        passage.setWork(requireWork(request.workId()));
        passage.setLocator(request.locator());
        passage.setTextContent(request.textContent());
        passage.setPageNumber(request.pageNumber());
        return mapper.toDto(passage);
    }

    @Transactional
    public void deletePassage(Long id) {
        passageRepository.delete(requirePassage(id));
    }

    // ----- Utilidades compartidas ----------------------------------------

    Philosopher requirePhilosopher(Long id) {
        return philosopherRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("filosofo", id));
    }

    Work requireWork(Long id) {
        return workRepository.findWithPhilosopherById(id)
                .orElseThrow(() -> new NotFoundException("obra", id));
    }

    Passage requirePassage(Long id) {
        return passageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("pasaje", id));
    }

    private void applyAdversary(Work work, Long adversaryId) {
        if (adversaryId == null) {
            work.setDirectAdversary(null);
            return;
        }
        if (adversaryId.equals(work.getId())) {
            throw new ConflictException("Una obra no puede ser adversaria de si misma");
        }
        work.setDirectAdversary(requireWork(adversaryId));
    }
}
