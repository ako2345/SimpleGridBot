package com.ako2345.simplegridbot.bot.grid;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.ako2345.simplegridbot.Constants.DEFAULT_SCALE;

/**
 * Класс для работы с ценовой сеткой. Определяет необходимость совершения ордеров при пересечении ценой ценовых
 * уровней сетки.
 */
public class GridManager {

    private final Grid grid;
    private final int lotsPerGrid;
    private BigDecimal activePriceLevel;
    private BigDecimal previousPrice;

    public GridManager(Grid grid, BigDecimal investment, BigDecimal initialPrice, BigDecimal lotSize) {
        this.grid = grid;
        this.previousPrice = initialPrice;
        this.lotsPerGrid = calculateLotsPerGrid(grid, investment, lotSize);
        if (initialPrice.compareTo(grid.getUpperPrice()) > 0) {
            activePriceLevel = grid.getUpperPrice();
        } else if (initialPrice.compareTo(grid.getLowerPrice()) < 0) {
            activePriceLevel = grid.getLowerPrice();
        } else {
            activePriceLevel = BigDecimal.ZERO;
        }
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

    /**
     * Обрабатывает новую информацию о цене и принимает решение о необходимости совершения ордера. Возвращает
     * количество необходимых ордеров на минимальное количество лотов.
     *
     * @return 0, если не требуется предпринимать никаких действий. Положительное число, если требуются покупки,
     * отрицательное число, если требуются продажи.
     */
    public int processPrice(BigDecimal currentPrice) {
        int timesToBuyOrSell = 0;
        var currentPriceRangeIndex = grid.getPriceRangeIndex(currentPrice);
        var previousPriceRangeIndex = grid.getPriceRangeIndex(previousPrice);
        // Проверка пересечения ценового уровня сетки
        if (currentPriceRangeIndex != previousPriceRangeIndex) {
            var crossedPriceLevelIndex = currentPriceRangeIndex > previousPriceRangeIndex ? currentPriceRangeIndex : currentPriceRangeIndex + 1;
            var crossedPriceLevel = grid.getPriceLevels()[crossedPriceLevelIndex];
            // Проверка на то, что ранее не было пересечения этого ценового уровня сетки
            if (crossedPriceLevel.compareTo(activePriceLevel) != 0) {
                // Если было пересечено несколько уровней, то нужно кратно увеличить количество лотов для ордера
                var multiplier = currentPriceRangeIndex > previousPriceRangeIndex ?
                        currentPriceRangeIndex - previousPriceRangeIndex - (previousPrice.compareTo(activePriceLevel) < 0 ? 1 : 0) :
                        previousPriceRangeIndex - currentPriceRangeIndex - (previousPrice.compareTo(activePriceLevel) > 0 ? 1 : 0);
                if (currentPriceRangeIndex > previousPriceRangeIndex) {
                    timesToBuyOrSell = -multiplier;
                } else {
                    timesToBuyOrSell = multiplier;
                }
                activePriceLevel = crossedPriceLevel;
            }
        }
        previousPrice = currentPrice;
        return timesToBuyOrSell;
    }

    public int getLotsPerGrid() {
        return lotsPerGrid;
    }

}