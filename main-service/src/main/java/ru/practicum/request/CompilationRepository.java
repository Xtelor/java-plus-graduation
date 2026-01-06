package ru.practicum.request;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.request.model.Compilation;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {
}
