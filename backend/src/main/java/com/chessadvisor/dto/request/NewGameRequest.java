package com.chessadvisor.dto.request;

import com.chessadvisor.entity.PlayerColor;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewGameRequest {
    @NotNull
    private PlayerColor playerColor;
}
