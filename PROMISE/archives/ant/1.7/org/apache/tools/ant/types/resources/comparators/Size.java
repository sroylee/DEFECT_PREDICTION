package org.apache.tools.ant.types.resources.comparators;

import org.apache.tools.ant.types.Resource;

/**
 * Compares Resources by size.
 * @since Ant 1.7
 */
public class Size extends ResourceComparator {
    /**
     * Compare two Resources.
     * @param foo the first Resource.
     * @param bar the second Resource.
     * @return a negative integer, zero, or a positive integer as the first
     *         argument is less than, equal to, or greater than the second.
     */
    protected int resourceCompare(Resource foo, Resource bar) {
        return (int) (foo.getSize() - bar.getSize());
    }

}
