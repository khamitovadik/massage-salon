package com.salon.service;

import com.salon.dto.request.CreateServiceRequest;
import com.salon.dto.request.UpdateServiceRequest;
import com.salon.dto.response.ServiceResponse;
import com.salon.entity.SalonService;
import com.salon.repository.SalonServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SalonServiceService {

    private final SalonServiceRepository repository;

    public ServiceResponse create(CreateServiceRequest req) {
        SalonService service = SalonService.builder()
            .name(req.getName())
            .description(req.getDescription())
            .price(req.getPrice())
            .durationMinutes(req.getDurationMinutes())
            .build();
        return ServiceResponse.from(repository.save(service));
    }

    public List<ServiceResponse> getAllActive() {
        return repository.findAllByActiveTrue()
            .stream().map(ServiceResponse::from).toList();
    }

    public List<ServiceResponse> getAll() {
        return repository.findAll()
            .stream().map(ServiceResponse::from).toList();
    }

    public ServiceResponse getById(Long id) {
        return ServiceResponse.from(findOrThrow(id));
    }

    @Transactional
    public ServiceResponse update(Long id, UpdateServiceRequest req) {
        SalonService service = findOrThrow(id);

        if (req.getName() != null)            service.setName(req.getName());
        if (req.getDescription() != null)     service.setDescription(req.getDescription());
        if (req.getPrice() != null)           service.setPrice(req.getPrice());
        if (req.getDurationMinutes() != null) service.setDurationMinutes(req.getDurationMinutes());
        if (req.getActive() != null)          service.setActive(req.getActive());

        return ServiceResponse.from(repository.save(service));
    }

    @Transactional
    public void deactivate(Long id) {
        SalonService service = findOrThrow(id);
        service.setActive(false);
        repository.save(service);
    }

    private SalonService findOrThrow(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Услуга не найдена: " + id));
    }
}
