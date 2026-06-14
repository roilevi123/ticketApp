package com.ticketing.ticketapp.Domain.Event;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class MapAreaConverter implements AttributeConverter<MapArea[][], String> {

    @Override
    public String convertToDatabaseColumn(MapArea[][] map) {
        if (map == null || map.length == 0) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < map.length; i++) {
            if (i > 0) sb.append("|");
            if (map[i] == null) continue;
            for (int j = 0; j < map[i].length; j++) {
                if (j > 0) sb.append(",");
                sb.append(map[i][j] != null ? map[i][j].name() : "SEAT");
            }
        }
        return sb.toString();
    }

    @Override
    public MapArea[][] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) return null;
        String[] rows = dbData.split("\\|", -1);
        MapArea[][] map = new MapArea[rows.length][];
        for (int i = 0; i < rows.length; i++) {
            if (rows[i].isEmpty()) {
                map[i] = new MapArea[0];
                continue;
            }
            String[] cols = rows[i].split(",", -1);
            map[i] = new MapArea[cols.length];
            for (int j = 0; j < cols.length; j++) {
                try {
                    map[i][j] = MapArea.valueOf(cols[j]);
                } catch (IllegalArgumentException e) {
                    map[i][j] = MapArea.SEAT;
                }
            }
        }
        return map;
    }
}
