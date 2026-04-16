package com.pulse.desktop.repo;

import com.pulse.desktop.model.FeatureMatrixRow;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FeatureMatrixRepository {

    public List<FeatureMatrixRow> loadRows() throws IOException {
        InputStream input = FeatureMatrixRepository.class.getResourceAsStream("/feature-matrix.csv");
        if (input == null) {
            return List.of();
        }

        List<FeatureMatrixRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            boolean header = true;
            while ((line = reader.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }
                String[] parts = line.split(",", 4);
                if (parts.length < 4) {
                    continue;
                }
                rows.add(new FeatureMatrixRow(parts[0], parts[1], parts[2], parts[3]));
            }
        }
        return rows;
    }
}
