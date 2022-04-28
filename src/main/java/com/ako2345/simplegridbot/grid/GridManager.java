package com.ako2345.simplegridbot.grid;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.ako2345.simplegridbot.Constants.DEFAULT_SCALE;

/**
 * Класс для работы с ценовой сеткой. Определяет необходимость совершения ордеров при пересечении цены ценовых
 * уровней сетки.
 */
public class GridManager {

    private final Grid grid;
    private final int lotsPerGrid;
    private BigDecimal activePriceLevel = BigDecimal.ZERO;
    private BigDecimal previousPrice;

    public GridManager(Grid grid, BigDecimal investment, BigDecimal initialPrice, BigDecimal lotSize) {
        this.grid = grid;
        this.previousPrice = initialPrice;
        this.lotsPerGrid = calculateLotsPerGrid(grid, investment, lotSize);
        if (lotsPerGrid == 0) throw new IllegalArgumentException("Not enough investment");
    }

    /**
     * Вычисление количества лотов, которые будут продавться или покупаться при пересечении ценой ченовых уровней сетки.
     */
    public static int calculateLotsPerGrid(Grid grid, BigDecimal investment, BigDecimal lotSize) {
        var gridsNumber = grid.getGridsNumber();
        var priceLevels = grid.getPriceLevels();
        var averageBuyPrice = priceLevels[0].add(priceLevels[gridsNumber - 2]).divide(new BigDecimal(2), DEFAULT_SCALE, RoundingMode.DOWN);
        return investment.divide(
                averageBuyPrice.multiply(lotSize).multiply(new BigDecimal(gridsNumber - 1)),
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

    public Signal processPrice(BigDecimal currentPrice) {
        Signal signal = Signal.NONE;
        var currentPriceRangeIndex = grid.getPriceRangeIndex(currentPrice);
        var previousPriceRangeIndex = grid.getPriceRangeIndex(previousPrice);
        if (currentPriceRangeIndex != previousPriceRangeIndex) {
            // Пересечён ценовой уровень сетки
            var crossedPriceLevelIndex = Math.max(currentPriceRangeIndex, previousPriceRangeIndex);
            var crossedPriceLevel = grid.getPriceLevel(crossedPriceLevelIndex);
            if (!crossedPriceLevel.equals(activePriceLevel)) {
                // Пересечён новый уровень: возвращаем сигнал для ордера
                if (currentPriceRangeIndex > previousPriceRangeIndex) {
                    signal = Signal.SELL;
                } else {
                    signal = Signal.BUY;
                }
                activePriceLevel = crossedPriceLevel;
            }
        }
        previousPrice = currentPrice;
        return signal;
    }

    public int getLotsPerGrid() {
        return lotsPerGrid;
    }

    public Grid getGrid() {
        return grid;
    }

}