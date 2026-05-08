package Domain.Event;

public record MapAreaDTO(
        int row,
        int col,
        boolean isAvailable,
        String areaName
) {

}