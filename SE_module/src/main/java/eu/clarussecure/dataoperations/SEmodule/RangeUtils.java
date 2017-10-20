package eu.clarussecure.dataoperations.SEmodule;

/*******************************************************************************
 * Copyright (c) 2017, EURECOM
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     - Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 *     - Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *     - Neither the name of EURECOM nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Contact: Monir AZRAOUI, Melek ÖNEN, Refik MOLVA
 * name.surname(at)eurecom(dot)fr
 *
 *******************************************************************************/

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;

public class RangeUtils {

    public static int checkContent(String[] attributes, String[][] content) {
        int supportsNumericColumns = 0;
        int col = content[0].length;
        int j = 0;
        for (int i = 0; i < col; i++) {
            final int ii = i;
            if (StringUtils.isNumeric(content[0][i])) {
                int ll = content[0][i].length();
                int[] array = IntStream.range(0, content.length).map(k -> Integer.parseInt(content[k][ii])).toArray();
                Arrays.sort(array);
                String[] arrayS = new String[array.length];
                for (int kk = 0; kk < array.length; kk++) {
                    arrayS[kk] = String.format("%0" + ll + "d", array[kk]);
                }
                // num_Attr stores information a numerical attribute: name:min:max:nomberformat
                Store.num_Attr.put(attributes[i], arrayS[0] + ":" + arrayS[arrayS.length - 1] + ":" + ll);
                supportsNumericColumns++;
                j++;
            }
        }
        return supportsNumericColumns;
    }

    public static void askUserForRangeFeature() {
        System.out.println("Do you want to include range queries feature ? [Y/N]");
        Scanner sc = new Scanner(System.in);
        String answer = sc.nextLine();

        switch (answer.toUpperCase()) {
        case "Y":
            askUserForRangesConfiguration();
            break;

        case "N":
            break;

        default:
            askUserForRangeFeature();
        }
    }

    private static void askUserForRangesConfiguration() {
        System.out.println("\nChoose one of the following options:");
        System.out.println("  display	: Display the current ranges configuration");
        System.out.println("  add 		: Add a new range configuration");
        System.out.println("  delete	: Remove a range configuration");
        System.out.println("  save		: Save the range configuration and proceed");
        Scanner sc = new Scanner(System.in);
        String answer = sc.nextLine();

        switch (answer) {
        case "display":
            displayRangesConfig();
            askUserForRangesConfiguration();
            break;

        case "add":
            askUserForRange();
            System.out.println("[!] The range config. has been added successfully!");
            askUserForRangesConfiguration();
            break;

        case "delete":
            askUserForRangeDelete();
            System.out.println("[!] The range config. has been deleted successfully!");
            askUserForRangesConfiguration();
            break;

        case "save":
            System.out.println("[!] The range config. has been saved successfully!");
            break;

        default:
            askUserForRangesConfiguration();
        }
    }

    private static void displayRangesConfig() {
        if (Store.ranges.keySet().isEmpty())
            System.out.println("No ranges configuration to display");
        else {
            System.out.println("Ranges configuration added:");

            //Display ranges configuration
            String[] header = { "attribute name", "initial value", "range length" };
            String[][] ranges = new String[Store.ranges.size()][3];
            int i = 0;
            for (Iterator<String> it = Store.ranges.keySet().iterator(); it.hasNext();) {
                String a = it.next();
                ranges[i][0] = a;
                ranges[i][1] = Store.ranges.get(a).split(":")[0];
                ranges[i][2] = Store.ranges.get(a).split(":")[1];
                i++;
                //System.out.println("[" + a + " : " + Store.ranges.get(a).split(":")[0] + " : " + Store.ranges.get(a).split(":")[1] +"]");
            }

            System.out.println();
        }
    }

    private static void askUserForRange() {
        System.out.println("The selected database contains the following numerical columns:");
        String[] header = { "attribute name", "min value", "max value" };
        //System.out.println("[attribute name : min value : max value]");
        String[][] numCol = new String[Store.num_Attr.size()][3];
        Set<String> attributes = Store.num_Attr.keySet();
        int i = 0;
        for (Iterator<String> it = attributes.iterator(); it.hasNext();) {
            String a = it.next();
            numCol[i][0] = a;
            numCol[i][1] = Store.num_Attr.get(a).split(":")[0];
            numCol[i][2] = Store.num_Attr.get(a).split(":")[1];
            i++;
            //System.out.println("[" + a + " : " + Store.ranges.get(a).split(":")[0] + " : " + Store.ranges.get(a).split(":")[1] +"]");
        }

        System.out.println();
        System.out.println("Please enter your choice as follow: attribute name, initial value, range");
        Scanner sc = new Scanner(System.in);
        String answer = sc.nextLine();
        String rangeConfig = validRangeConf(answer);
        if (rangeConfig != null) {
            Store.ranges.put(rangeConfig.split(",")[0], rangeConfig.split(",")[1]);
        } else {
            System.out.println("[!] Invalid range configuration\n");
            askUserForRange();
        }
    }

    private static String validRangeConf(String userInput) {
        String[] rangeConfig = userInput.split(",");
        if (rangeConfig.length != 3)
            return null;
        Pattern p = Pattern.compile("\'([^\']*)\'");
        Matcher m = p.matcher(rangeConfig[0]);
        if (m.find())
            rangeConfig[0] = m.group(1);
        else
            rangeConfig[0] = rangeConfig[0].replaceAll(" ", "");
        rangeConfig[1] = rangeConfig[1].replaceAll(" ", "");
        rangeConfig[2] = rangeConfig[2].replaceAll(" ", "");

        if (rangeConfig.length == 3) {
            if (Store.num_Attr.keySet().contains(rangeConfig[0]) && rangeConfig[1].matches("^-?\\d+$")
                    && rangeConfig[2].matches("^-?\\d+$")) {
                if (Integer.valueOf(rangeConfig[1]) < 0 || Integer.valueOf(rangeConfig[2]) <= 0)
                    return null;
                if (Integer.valueOf(rangeConfig[1]) > Integer
                        .valueOf(Store.num_Attr.get(rangeConfig[0]).split(":")[0])) {
                    System.out.println("ERROR: Initial value must be lower or equal to the minimum value ["
                            + Store.num_Attr.get(rangeConfig[0]).split(":")[0] + "]");
                    return null;
                }
                String validRangeConfig = rangeConfig[0] + "," + rangeConfig[1] + ":" + rangeConfig[2];
                return validRangeConfig;
            } else
                return null;
        } else
            return null;
    }

    public static void askUserForRangeDelete() {
        System.out.println("The attributes concerned by range queries are");
        System.out.println(Store.ranges.keySet());
        System.out.println("Please enter the attribute name in order to delete the range config.");
        Scanner sc = new Scanner(System.in);
        String answer = sc.nextLine();
        if (Store.ranges.keySet().contains(answer)) {
            Store.ranges.remove(answer);
        } else {
            System.out.println("[!] Invalid attribute name!\n");
            askUserForRangeDelete();
        }
    }

    public static String[][] updateDB(String[] attributes, String[][] contents) {
        int col = contents[0].length;
        int row = contents.length;
        int additionalCol = Store.ranges.size();
        int newCol = col + additionalCol;
        String[][] newContents = new String[row + 1][newCol];
        String[] newAttrNames = new String[newCol];
        String[] newAttr = new String[additionalCol];
        String[][] newColumns = new String[row][additionalCol];

        int i = 0;
        for (Iterator<String> it = Store.ranges.keySet().iterator(); it.hasNext();) {
            String a = it.next();
            newAttr[i] = "RANGE_" + a;
            int col_index = Arrays.asList(attributes).indexOf(a);
            for (int r = 0; r < row; r++) {
                int ll = contents[r][col_index].length();
                int value = Integer.valueOf(contents[r][col_index]);
                int initial = Integer.valueOf(Store.ranges.get(a).split(":")[0]);
                int range = Integer.valueOf(Store.ranges.get(a).split(":")[1]);
                newColumns[r][i] = getRange(value, initial, range, ll);
            }
            i++;
        }

        newAttrNames = Arrays.copyOf(attributes, newCol);
        System.arraycopy(newAttr, 0, newAttrNames, col, additionalCol);
        newContents[0] = Arrays.copyOf(newAttrNames, newCol);
        for (int r = 1; r < row + 1; r++) {
            newContents[r] = Arrays.copyOf(contents[r - 1], newCol);
            System.arraycopy(newColumns[r - 1], 0, newContents[r], col, additionalCol);
        }
        return newContents;
    }

    private static String getRange(int value, int init_value, int range, int nb_char) {
        List<Integer> boundaries = new ArrayList<Integer>();
        if (value == init_value) {
            boundaries.add(init_value);
            boundaries.add(init_value + range - 1);
        } else {
            int min = ((value - init_value) / range) * range + init_value;
            int max = min + range - 1;
            boundaries.add(min);
            boundaries.add(max);
        }
        return String.format("%0" + nb_char + "d", Collections.min(boundaries)) + "-"
                + String.format("%0" + nb_char + "d", Collections.max(boundaries));
    }

}