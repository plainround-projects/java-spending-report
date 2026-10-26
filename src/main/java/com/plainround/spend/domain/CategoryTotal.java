package com.plainround.spend.domain;

import java.math.BigDecimal;

/**
 * The exact sum spent in one category.
 *
 * @param category the category name
 * @param total the exact, unrounded sum
 */
public record CategoryTotal(String category, BigDecimal total) {}
