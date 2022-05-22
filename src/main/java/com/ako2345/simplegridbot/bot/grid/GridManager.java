package com.ako2345.simplegridbot.bot.grid;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.ako2345.simplegridbot.Constants.DEFAULT_SCALE;

/**
 * Класс для работы с ценовой сеткой.
 */
public class GridManager {

    private final Grid grid;
    private final int lotsPerGrid;

    public GridManager(Grid grid, BigDecimal investment, BigDecimal lotSize) {
        this.grid = grid;
        this.lotsPerGrid = calculateLotsPerGrid(grid, investment, lotSize);
        if (lotsPerGrid == 0) throw new IllegalArgumentException("Not enough investment");
    }

    /**
     * Вычисление количества лотов, которые будут продавться или покупаться при пересечении ценой ченовых уровней сетки.
     */
    public static int calculateLotsPerGrid(Grid grid, BigDecimal investment, BigDecimal lotSize) {
        var gridsNumber = grid.getGridsNumber();
        var priceLevels = grid.getPriceLevels();
        var averageBuyPrice = priceLevels[0]
                .add(priceLevels[gridsNumber - 2])
                .divide(BigDecimal.valueOf(2), DEFAULT_SCALE, RoundingMode.DOWN);
        return investment.divide(
                averageBuyPrice.multiply(lotSize).multiply(BigDecimal.valueOf(gridsNumber - 1)),
                RoundingMode.DOWN
        ).intValue();
    }

    /**
     * Количества лотов, которые необходимо купить перед началом работы алгоритма.
     */
    public int lotsToBuyOnStart(BigDecimal currentPrice) {
        var gridsNumber = grid.getGridsNumber();
        var currentPriceLevel = grid.getPriceRangeIndex(currentPrice);
        if (currentPriceLevel == gridsNumber - 1) return 0;
        if (currentPriceLevel <= 0) return (gridsNumber - 1) * lotsPerGrid;
        return (gridsNumber - 1 - currentPriceLevel) * lotsPerGrid;
    }

    public int getLotsPerGrid() {
        return lotsPerGrid;
    }

    public Grid getGrid() {
        return grid;
    }

}