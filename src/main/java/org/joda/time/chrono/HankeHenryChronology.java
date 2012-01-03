/*
 *  Copyright 2012 Dario Diaz
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.joda.time.chrono;

import java.util.HashMap;
import java.util.Map;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;

/**
 * Implements the Hanke-Henry Permanent Calendar system, wich defines every year as identical.
 * Every 5 or 6 years there is a one-week long "Mini-Month," called "Xtr (or Extra)," at the end of December. 
 * In this implementation, the Xtra week is in December.
 * <p>
 * Although the Hanke-Henry Permanent Calendar did not exist before 2003 Gregorian, this
 * chronology assumes it did, thus it is proleptic.
 * <p>
 * TODO:<br> 
 * This calendar system uses UTC everywhere (2nd FAQ).<br>
 * Clean code.
 * 
 * @see <a href="http://henry.pha.jhu.edu/calendar.html">Official Web</a>
 * @author Dar&iacute;o D&iacute;az
 */
public class HankeHenryChronology extends BasicChronology {

    private static final int[] DAYS_PER_MONTH_ARRAY = {
        30,30,31,30,30,31,30,30,31,30,30,31
    };
    
    private static final int[] MAX_DAYS_PER_MONTH_ARRAY = {
        30,30,31,30,30,31,30,30,31,30,30,38
    };
    
    /** The lowest year that can be fully supported. */
    private static final int MIN_YEAR = -292275054;

    /** The highest year that can be fully supported. */
    private static final int MAX_YEAR = 292278993;
    
    /** Cache of zone to chronology arrays */
    private static final Map<DateTimeZone, HankeHenryChronology[]> cCache = new HashMap<DateTimeZone, HankeHenryChronology[]>();
    
    private static final double AVG_DAYS_PER_YEAR = 4382 / 12;
    
    /** Singleton instance of a UTC HankeHenryChronology */
    private static final HankeHenryChronology INSTANCE_UTC;
    static {
        // init after static fields
        INSTANCE_UTC = getInstance(DateTimeZone.UTC);
    }
    
    /**
     * Gets an instance of the HankeHenryChronology.
     * The time zone of the returned instance is UTC.
     * 
     * @return a singleton UTC instance of the chronology
     */
    public static HankeHenryChronology getInstanceUTC() {
        return INSTANCE_UTC;
    }

    /**
     * Gets an instance of the HankeHenryChronology in the default time zone.
     * 
     * @return a chronology in the default time zone
     */
    public static HankeHenryChronology getInstance() {
        return getInstance(DateTimeZone.getDefault(), 7);
    }

    /**
     * Gets an instance of the HankeHenryChronology in the given time zone.
     * 
     * @param zone  the time zone to get the chronology in, null is default
     * @return a chronology in the specified time zone
     */
    public static HankeHenryChronology getInstance(DateTimeZone zone) {
        return getInstance(zone, 7);
    }

    /**
     * Gets an instance of the HankeHenryChronology in the given time zone.
     * 
     * @param zone  the time zone to get the chronology in, null is default
     * @param minDaysInFirstWeek  minimum number of days in first week of the year; always is 7
     * @return a chronology in the specified time zone
     */
    public static HankeHenryChronology getInstance(DateTimeZone zone, int minDaysInFirstWeek) {
        if (zone == null) {
            zone = DateTimeZone.getDefault();
        }
        HankeHenryChronology chrono;
        synchronized (cCache) {
            HankeHenryChronology[] chronos = cCache.get(zone);
            if (chronos == null) {
                chronos = new HankeHenryChronology[7];
                cCache.put(zone, chronos);
            }
            try {
                chrono = chronos[minDaysInFirstWeek - 1];
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new IllegalArgumentException
                    ("Invalid min days in first week: " + minDaysInFirstWeek);
            }
            if (chrono == null) {
                if (zone == DateTimeZone.UTC) {
                    // First create without a lower limit.
                    chrono = new HankeHenryChronology(null, null, minDaysInFirstWeek);
                    // Impose lower limit and make another HankeHenryChronology.
                    DateTime lowerLimit = new DateTime(1, 1, 1, 0, 0, 0, 0, chrono);
                    chrono = new HankeHenryChronology
                        (LimitChronology.getInstance(chrono, lowerLimit, null),
                         null, minDaysInFirstWeek);
                } else {
                    chrono = getInstance(DateTimeZone.UTC, minDaysInFirstWeek);
                    chrono = new HankeHenryChronology
                        (ZonedChronology.getInstance(chrono, zone), null, minDaysInFirstWeek);
                }
                chronos[minDaysInFirstWeek - 1] = chrono;
            }
        }
        return chrono;
    }
    
    /**
     * Constructor.
     */
    HankeHenryChronology(Chronology base, Object param, int minDaysInFirstWeek) {
        super(base, param, minDaysInFirstWeek);
    }
    
    @Override
    int getMonthOfYear(long millis, int year) {
        int doyZeroBased = (int) ((millis - getYearMillis(year)) / DateTimeConstants.MILLIS_PER_DAY);
        if (doyZeroBased >= 364) {
            return 12;
        } 
        
        return ((doyZeroBased * 3) / 91) + 1;
    }

    @Override
    long getYearDifference(long minuendInstant, long subtrahendInstant) {
        int minuendYear = getYear(minuendInstant);
        int subtrahendYear = getYear(subtrahendInstant);
    
        // Inlined remainder method to avoid duplicate calls to get.
        long minuendRem = minuendInstant - getYearMillis(minuendYear);
        long subtrahendRem = subtrahendInstant - getYearMillis(subtrahendYear);
    
        // Balance leap year differences on remainders.
        if (subtrahendRem >= 364) {
            if (isLeapYear(subtrahendYear)) {
                if (!isLeapYear(minuendYear)) {
                    subtrahendRem -= DateTimeConstants.MILLIS_PER_DAY;
                }
            } else if (minuendRem >= 364 && isLeapYear(minuendYear)) {
                minuendRem -= DateTimeConstants.MILLIS_PER_DAY;
            }
        }
    
        int difference = minuendYear - subtrahendYear;
        if (minuendRem < subtrahendRem) {
            difference--;
        }
        return difference;
    }

    @Override
    boolean isLeapYear(int year) {
        // if newton = 1 then the year contains a Xtr (or Extra) Week
        int newton=0;
        int i = year-1;
        
        while(i<=year) {
            int idays = i*365+i/4-i/100+i/400;
            int nweeks = idays/7;
            int iremainder = idays-nweeks*7;
            
            if(iremainder==4 && i==year || 
                    iremainder==3 && i==year-1) {
                newton=1;
            }
            
            i++;
        }
        
        return newton == 1;
    }

    @Override
    int getDaysInYearMonth(int year, int month) {
        if(isLeapYear(year)) {
            return getDaysInMonthMax(month);
        } else {
            return DAYS_PER_MONTH_ARRAY[month-1];
        }
    }

    @Override
    int getDaysInMonthMax(int month) {
        return MAX_DAYS_PER_MONTH_ARRAY[month-1];
    }
    
    /**
     * Gets the maximum number of days in the month specified by the instant.
     * 
     * @param instant  millis from 1970-01-01T00:00:00Z
     * @return the maximum number of days in the month
     */
    @Override
    int getDaysInMonthMax(long instant) {
        int thisYear = getYear(instant);
        int thisMonth = getMonthOfYear(instant, thisYear);
        return getDaysInYearMonth(thisYear, thisMonth);
    }

    @Override
    long getTotalMillisByYearMonth(int year, int month) {
        long days = 0;
        int[] arrayToUse = isLeapYear(year) ? MAX_DAYS_PER_MONTH_ARRAY: DAYS_PER_MONTH_ARRAY;
        
        for(int i=0; i<month-1;i++){
            days += arrayToUse[i];
        }
        
        return days * DateTimeConstants.MILLIS_PER_DAY;
    }

    @Override
    long calculateFirstDayOfYearMillis(int year) {
        // Java epoch is 1970-01-01 Gregorian which, I guess, is 1969-12-29 Hanke-Henry.
        //I don't think this will work.
        int relativeYear = year - 1970;
        int leapYears=0;
        int i=0;
        if (relativeYear <= 0) {
            while(i>relativeYear){
                if(isLeapYear(i+1970)){
                    leapYears++;
                }
                i--;
            }
        } else {
            while(i<relativeYear){
                if(isLeapYear(i+1970)){
                    leapYears++;
                }
                i++;
            }
        }
        
        //change the -22L if 1969-12-29 Hanke-Henry is incorrect
        //yeah, 4 is a magic number.
        long millis = (relativeYear * 364L + leapYears*7L - 4L)
            * (long)DateTimeConstants.MILLIS_PER_DAY;
        
        return millis;
    }
    
    @Override
    int getMinYear() {
        return MIN_YEAR;
    }

    @Override
    int getMaxYear() {
        return MAX_YEAR;
    }

    @Override
    long getAverageMillisPerYear() {
        //I dunno, really. Meanwhile, mess with this.
        return (long) (AVG_DAYS_PER_YEAR * DateTimeConstants.MILLIS_PER_DAY);
    }

    @Override
    long getAverageMillisPerYearDividedByTwo() {
        return getAverageMillisPerYear()/2;
    }

    @Override
    long getAverageMillisPerMonth() {
        return (long)(30.333333334 * DateTimeConstants.MILLIS_PER_DAY);
    }

    @Override
    long getApproxMillisAtEpochDividedByTwo() {
        return (1969L * getAverageMillisPerYear()  + 4L * DateTimeConstants.MILLIS_PER_DAY) / 2;
    }

    @Override
    long setYear(long instant, int year) {
        //maybe this will work
        int thisYear = getYear(instant);
        int dayOfYear = getDayOfYear(instant, thisYear);
        int millisOfDay = getMillisOfDay(instant);

        if (dayOfYear > 364) {
            // Current year is leap, and day is leap.
            if (!isLeapYear(year)) {
                // Moving to a non-leap year, leap day doesn't exist.
                dayOfYear--;
            }
        }

        instant = getYearMonthDayMillis(year, 1, dayOfYear);
        instant += millisOfDay;

        return instant;
    }

    @Override
    public Chronology withUTC() {
        return INSTANCE_UTC;
    }

    @Override
    public Chronology withZone(DateTimeZone zone) {
        if (zone == null) {
            zone = DateTimeZone.getDefault();
        }
        if (zone == getZone()) {
            return this;
        }
        return getInstance(zone);
    }
    
    /**
     * Get the number of days in the year.
     *
     * @return 371
     */
    @Override
    int getDaysInYearMax() {
        return 371;
    }

    /**
     * Get the number of days in the year.
     *
     * @param year  the year to use
     * @return 371 if a leap year, otherwise 364
     */
    @Override
    int getDaysInYear(int year) {
        return isLeapYear(year) ? 371 : 364;
    }
    
    @Override
    int getMaxMonth() {
        return 12;
    }
    
    /**
     * Get the number of weeks in the year.
     *
     * @param year  the year to use
     * @return number of weeks in the year
     */
    @Override
    int getWeeksInYear(int year) {
        return isLeapYear(year) ? 53:52;
    }
    
    /**
     * @param instant millis from 1970-01-01T00:00:00Z
     */
    @Override
    int getYear(long instant) {
        // Get an initial estimate of the year, and the millis value that
        // represents the start of that year. Then verify estimate and fix if
        // necessary.

        // Initial estimate uses values divided by two to avoid overflow.
        long unitMillis = getAverageMillisPerYearDividedByTwo();
        long i2 = (instant >> 1) + getApproxMillisAtEpochDividedByTwo();
        if (i2 < 0) {
            i2 = i2 - unitMillis + 1;
        }
        int year = (int) (i2 / unitMillis);

        long yearStart = getYearMillis(year);
        long diff = instant - yearStart;

        if (diff < 0) {
            year--;
        } else if (diff >= DateTimeConstants.MILLIS_PER_DAY * 364L) {
            // One year may need to be added to fix estimate.
            long oneYear;
            if (isLeapYear(year)) {
                oneYear = DateTimeConstants.MILLIS_PER_DAY * 371L;
            } else {
                oneYear = DateTimeConstants.MILLIS_PER_DAY * 364L;
            }

            yearStart += oneYear;

            if (yearStart <= instant) {
                // Didn't go too far, so actually add one year.
                year++;
            }
        }

        return year;
    }
    
    @Override
    protected void assemble(Fields fields) {
        if (getBase() == null) {
            super.assemble(fields);

            fields.monthOfYear = new BasicMonthOfYearDateTimeField(this, 12);
            fields.months = fields.monthOfYear.getDurationField();
        }
    }
}
