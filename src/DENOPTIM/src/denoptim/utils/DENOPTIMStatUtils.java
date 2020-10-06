/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no>
 * 
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published
 *   by the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package denoptim.utils;

/**
 * Utilities for calculating basic statistics
 * @author Vishwesh Venkatraman
 */
public final class DENOPTIMStatUtils
{

//------------------------------------------------------------------------------

    /**
     * Returns the sum number in the numbers list.
     *
     * @param numbers list/array of numbers to calculate the sum of.
     * @return the sum of the numbers.
     */
    public static double sum(double[] numbers)
    {
        double sum = 0;
        for (double number : numbers) {
            sum += number;
        }
        return sum;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the mean number in the numbers list.
     *
     * @param numbers list/array of numbers to calculate the mean of.
     * @return the mean of the numbers.
     */
    public static double mean(double[] numbers)
    {
        double sum = sum(numbers);
        return sum / numbers.length;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the minimum value among the numbers .
     *
     * @param numbers list/array of numbers to calculate the mean of.
     * @return the min number in the numbers list.
     */
    public static double min(double[] numbers)
    {
        double min = Integer.MAX_VALUE;
        for (double number : numbers) {
            if (number < min)
                min = number;
        }
        return min;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the maximum value among the numbers .
     *
     * @param numbers list/array of numbers to calculate the max of.
     * @return the max number in the numbers list.
     */
    public static double max(double[] numbers)
    {
        double max = Integer.MIN_VALUE;
        for (double number : numbers) {
            if (number > max)
                max = number;
        }
        return max;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the standard deviation of the numbers. NaN is returned if the
     * numbers list is empty.
     *
     * @param numbers the numbers to calculate the standard deviation.
     * @return the standard deviation
     */
    public static double stddev(double[] numbers)
    {
        double stddev = Double.NaN;
        int n = numbers.length;
        if (n > 0)
        {
            if (n > 1)
            {
                stddev = Math.sqrt(var(numbers));
            }
            else
            {
                stddev = 0.0;
            }
        }
        return stddev;
    }

//------------------------------------------------------------------------------

    /**
     * Computes the variance of the available values. The unbiased
     * "sample variance" definitional formula is used: variance =
     * sum((x_i - mean)^2) / (n - 1).
     *
     * @param numbers the numbers to calculate the variance.
     * @return the variance of the numbers.
     */
    public static double var(double[] numbers)
    {
        int n = numbers.length;
        if (n == 0)
            return Double.NaN;
        else if (n == 1)
            return 0d;

        double mean = mean(numbers);
        double[] squares = new double[numbers.length];
        for (int i=0; i<numbers.length; i++)
        {
            double XminMean = numbers[i] - mean;
            squares[i] = Math.pow(XminMean, 2);
        }
        double sum = sum(squares);
        return sum / (n - 1);
    }

//------------------------------------------------------------------------------

    /**
     * Calculates median value of a <i>sorted</i> list.
     *
     * @param m
     * @return median value of m
     */
    public static double median(double[] m)
    {
        int middle = m.length / 2;  // subscript of middle element
        if (m.length % 2 == 1)
        {
            // Odd number of elements -- return the middle one.
            return m[middle];
        }
        else
        {
            // Even number -- return average of middle two
            // Must cast the numbers to double before dividing.
            return (m[middle-1] + m[middle]) / 2.0;
        }
    }

//------------------------------------------------------------------------------
    /**
     * Computes the skewness using the adjusted Fisher-Pearson skewness
     * coefficient.
     * @param m sample
     * @return skewness of sample or NaN if sample size is strictly less than 4.
     */
    public static double skewness(double[] m)
    {
        double n = m.length;
        if (n <= 3) {
            return Double.NaN;
        }
        double sampleSizeCorrectionTerm = Math.sqrt(n * (n - 1)) / (n - 2);
        double biasedStandardDeviation = Math.sqrt(n / (n - 1)) * stddev(m);
        double accDeviationFromMean = 0.0;
        double mean = mean(m);
        for (double v : m) {
            accDeviationFromMean += v - mean;
        }
        return sampleSizeCorrectionTerm
                * (accDeviationFromMean / (n * biasedStandardDeviation));
    }

//------------------------------------------------------------------------------

}
