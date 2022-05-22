package com.ako2345.simplegridbot.bot.grid;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.ako2345.simplegridbot.Constants.DEFAULT_SCALE;

/**
 * Ценовая сетка. Выделяет на оси цен {@link Grid#gridsNumber} уровней путём деления ценового диапазона от
 * {@link Grid#lowerPrice} до {@link Grid#upperPrice} на ({@link Grid#gridsNumber} - 1) равных частей.
 *
 * <p>Индекс минимального уровня: 0. Индекс максимального уровня: ({@link Grid#gridsNumber} - 1).
 *
 * <p>Ценовому диапазону от 0 до {@link Grid#lowerPrice} назначается индекс -1. Диапазон от {@link Grid#lowerPrice} до
 * ({@link Grid#lowerPrice} + {@link Grid#priceStep}) имеет индекс 0 и так далее. Диапазон от {@link Grid#upperPrice}
 * до бесконечности имеет индекс ({@link Grid#gridsNumber} - 1).
 */
public class Grid {

    private final BigDecimal lowerPrice;
    private final BigDecimal upperPrice;
    private final int gridsNumber;
    private final BigDecimal priceStep;
    private final BigDecimal[] priceLevels;

    public Grid(BigDecimal lowerPrice, BigDecimal upperPrice, int gridsNumber) {
        if (lowerPrice.signum() != 1 || upperPrice.signum() != 1)
            throw new IllegalArgumentException("Price must be positive");
        if (lowerPrice.compareTo(upperPrice) >= 0)
            throw new IllegalArgumentException("Lower price must be less than upper price");
        if (gridsNumber < 2)
            throw new IllegalArgumentException("Grids number must be more than 1");

        this.lowerPrice = lowerPrice;
        this.upperPrice = upperPrice;
        this.gridsNumber = gridsNumber;

        priceLevels = new BigDecimal[gridsNumber];
        priceStep = upperPrice.subtract(lowerPrice).divide(BigDecimal.valueOf(gridsNumber - 1), DEFAULT_SCALE, RoundingMode.DOWN);
        for (int i = 0; i < gridsNumber - 1; i++) {
            priceLevels[i] = lowerPrice.add(priceStep.multiply(BigDecimal.valueOf(i)));
        }
        priceLevels[gridsNumber - 1] = upperPrice;
    }

    /**
     * Возвращает индекс ценового диапазона сетки.
     *
     * @param price Цена, для которой определяется индекс.
     * @return От 0 до ({@link Grid#gridsNumber} - 1). Если цена выше максимальной цены сетки, то возвращается
     * ({@link Grid#gridsNumber} - 1). Если цена ниже минимальной цены сетки, то возвращается -1.
     */
    public int getPriceRangeIndex(BigDecimal price) {
        if (price.signum() == -1) throw new IllegalArgumentException("Price must be positive");
        if (price.compareTo(lowerPrice) < 0) return -1;
        if (price.compareTo(upperPrice) > 0) return gridsNumber - 1;
        return price.subtract(lowerPrice).divide(priceStep, RoundingMode.DOWN).intValue();
    }

    public BigDecimal[] getPriceLevels() {
        return priceLevels;
    }

    public BigDecimal getLowerPrice() {
        return lowerPrice;
    }

    public BigDecimal getUpperPrice() {
        return upperPrice;
    }

    public int getGridsNumber() {
        return gridsNumber;
    }

    public BigDecimal getPriceStep() {
        return priceStep;
    }

}