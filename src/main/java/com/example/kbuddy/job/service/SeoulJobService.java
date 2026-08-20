package com.example.kbuddy.job.service;

import com.example.kbuddy.job.client.SeoulJobClient;
import com.example.kbuddy.job.dto.SeoulJobApiEnvelope;
import com.example.kbuddy.job.dto.SeoulJobResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeoulJobService {

    private final SeoulJobClient seoulJobClient;

    public List<SeoulJobResponse> search(int startIndex, int endIndex) {
        SeoulJobApiEnvelope envelope = seoulJobClient.search(startIndex, endIndex);

        if (envelope == null || envelope.globalJobSearch() == null || envelope.globalJobSearch().row() == null) {
            return List.of();
        }

        return envelope.globalJobSearch().row().stream()
                .map(SeoulJobResponse::from)
                .toList();
    }
}
