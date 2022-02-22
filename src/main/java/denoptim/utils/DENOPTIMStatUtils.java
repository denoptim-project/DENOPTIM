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
     * Returns the standard deviation of the numbers.
     *
     * Double.NaN is returned if the numbers list is empty.
     *
     * @param numbers       the numbers to calculate the standard deviation.
     * @param biasCorrected true if variance is calculated by dividing by n - 1.
     *        False if by n. stddev is a sqrt of the variance.
     * @return the standard deviation
     */
    public static double stddev(double[] numbers, boolean biasCorrected)
    {
        double stddev = Double.NaN;
        int n = numbers.length;
        if (n > 0)
        {
            if (n > 1)
            {
                stddev = Math.sqrt(var(numbers, biasCorrected));
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
     * Computes the variance of the available values. By default, the unbiased
     * "sample variance" definitional formula is
     * used: variance = sum((x_i - mean)^2) / (n - 1)
     * <p/>
     * The "population variance"  ( sum((x_i - mean)^2) / n ) can also be
     * computed using this statistic.  The
     * <code>biasCorrected</code> property determines whether the "population"
     * or "sample" value is returned by the
     * <code>evaluate</code> and <code>getResult</code> methods. To compute
     * population variances, set this property to
     * <code>false</code>.
     *
     * @param numbers       the numbers to calculate the variance.
     * @param biasCorrected true if variance is calculated by dividing by n - 1.
     *                      False if by n.
     * @return the variance of the numbers.
     */
    public static double var(double[] numbers, boolean biasCorrected)
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
        return sum / (biasCorrected ? (n - 1) : n);
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
    * Computes the Kurtosis of the available values.
    * <p>
    * We use the following (unbiased) formula to define kurtosis:</p>
    *  <p>
    *  kurtosis = { [n(n+1) / (n -1)(n - 2)(n-3)] sum[(x_i - mean)^4] / std^4 } - [3(n-1)^2 / (n-2)(n-3)]
    *  </p><p>
    *  where n is the number of values, mean is the {@link Mean} and std is the
    * {@link StandardDeviation}</p>
    * <p>
    *  Note that this statistic is undefined for n < 4.  <code>Double.Nan</code>
    *  is returned when there is not sufficient data to compute the statistic.</p>
    *
    */

    public static double kurtosis(double[] m, boolean biasCorrected)
    {
        double value = Double.NaN;
        int N = m.length;
        if (N > 3)
        {
            double variance = var(m, biasCorrected);
            if (N <= 3 || variance < 10E-20)
            {
                value = 0.0;
            }
            else
            {
                double n = N;
                double f1 = (n*(n+1)) /((n -1) * (n - 2) * (n - 3));
                double avg = mean(m);
                double f2 = 0, f3 = 0;
                for (int i=0; i<N; i++)
                {
                    f2 += Math.pow((m[i] - avg), 4);
                    f3 += Math.pow((m[i] - avg), 2);
                }

                double f4 = (3 * (n-1) * (n -1)) / ((n-2)*(n-3));

                value = (f1 * (Math.pow(f2, 4)/Math.pow(f3, 2))) - f4;
            }
        }
        return value;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Computes the skewness of the available values.
     * @param m sample
     * @param biasCorrected  biasCorrected true if variance is calculated by 
     * dividing by n - 1. False if by n. stddev is a sqrt of the variance.
     * @return skewness of sample or NaN if sample size is strictly less than 4.
     */
    
    public static double skewness(double[] m, boolean biasCorrected)
    {
        double n = m.length;
        if (n <= 3) {
            return Double.NaN;
        }
        double sampleSizeCorrectionTerm = Math.sqrt(n * (n - 1)) / (n - 2);
        double biasedStandardDeviation = Math.sqrt(n / (n - 1)) 
                * stddev(m, biasCorrected);
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
