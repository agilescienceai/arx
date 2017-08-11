/*
 * ARX: Powerful Data Anonymization
 * Copyright 2012 - 2017 Fabian Prasser, Florian Kohlmayer and contributors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.deidentifier.arx.aggregates.utility;

import java.util.HashMap;
import java.util.Map;

import org.deidentifier.arx.DataHandleInternal;
import org.deidentifier.arx.common.Groupify;
import org.deidentifier.arx.common.TupleWrapper;
import org.deidentifier.arx.common.WrappedBoolean;

/**
 * Implementation of the Precision measure, as proposed in:<br>
 * <br>
 * L. Sweeney: "Achieving k-anonymity privacy protection using generalization and suppression"
 * J Uncertain Fuzz Knowl Sys 10 (5) (2002) 571-588.
 * 
 * @author Fabian Prasser
 */
public class UtilityModelColumnOrientedPrecision extends UtilityModel<UtilityMeasureColumnOriented> {

    /**
     * Creates a new instance
     * 
     * @param interrupt
     * @param input
     * @param output
     * @param groupedInput
     * @param groupedOutput
     * @param hierarchies
     * @param shares
     * @param indices
     * @param config
     */
    public UtilityModelColumnOrientedPrecision(WrappedBoolean interrupt,
                                               DataHandleInternal input,
                                               DataHandleInternal output,
                                               Groupify<TupleWrapper> groupedInput,
                                               Groupify<TupleWrapper> groupedOutput,
                                               String[][][] hierarchies,
                                               UtilityDomainShare[] shares,
                                               int[] indices,
                                               UtilityConfiguration config) {
        super(interrupt,
              input,
              output,
              groupedInput,
              groupedOutput,
              hierarchies,
              shares,
              indices,
              config);
    }
    
    @Override
    public UtilityMeasureColumnOriented evaluate() {
        
        // Prepare
        int[] indices = getIndices();
        DataHandleInternal output = getOutput();
        String[][][] hierarchies = getHierarchies();
        Map<String, Double>[] precisions = getPrecisions(hierarchies);
        double[] result = new double[indices.length];
        double[] min = new double[indices.length];
        double[] max = new double[indices.length];
        
        // For each column
        for (int i = 0; i < result.length; i++) {
            
            // Map
            int column = indices[i];
            
            // For each row
            for (int row = 0; row < output.getNumRows(); row++) {
                
                try {
                    double precision = 1d;
                    if (!isSuppressed(output, indices, row)) {
                        Double temp = precisions[i].get(output.getValue(row, column));
                        precision = temp != null ? temp : 1d;
                    }
                    result[i] += precision;
                } catch (Exception e) {
                    // Silently catch exceptions
                    result[i] = Double.NaN;
                    break;
                }
                
                // Check
                checkInterrupt();
            }
        }

        // For each column
        for (int i = 0; i < result.length; i++) {
            result[i] /= (double)output.getNumRows();
            min[i] = 0d;
            max[i] = 1d;
        }

        // Return
        return new UtilityMeasureColumnOriented(output, indices, min, result, max);
    }

    /**
     * Returns precisions
     * @param hierarchies
     * @return
     */
    private Map<String, Double>[] getPrecisions(String[][][] hierarchies) {

        // Prepare
        @SuppressWarnings("unchecked")
        Map<String, Double>[] precisions = new Map[hierarchies.length];
        
        for (int i=0; i<precisions.length; i++) {
            
            try {
                
                // Extract info
                String[][] hierarchy = hierarchies[i];
                
                // Calculate precision
                Map<String, Double> precision = new HashMap<String, Double>();
                for (int col = 0; col < hierarchy[0].length; col++) {
                    for (int row = 0; row < hierarchy.length; row++) {
                        String value = hierarchy[row][col];
                        if (!precision.containsKey(value)) {
                            precision.put(value, (double)col / ((double)hierarchy[0].length - 1d));
                        }
                    }
                    
                    // Check
                    checkInterrupt();
                }
                
                // Store
                precisions[i] = precision;
                
            } catch (Exception e) {
                
                // Drop silently
                precisions[i] = null;
            }
        }

        // Return
        return precisions;
    }
}