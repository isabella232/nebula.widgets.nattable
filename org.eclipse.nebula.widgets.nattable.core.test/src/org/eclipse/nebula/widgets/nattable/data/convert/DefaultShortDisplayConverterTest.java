/*******************************************************************************
 * Copyright (c) 2012 Original authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Original authors and others - initial API and implementation
 ******************************************************************************/
package org.eclipse.nebula.widgets.nattable.data.convert;

import org.junit.Assert;
import org.junit.Test;

public class DefaultShortDisplayConverterTest {

    private DefaultShortDisplayConverter shortConverter = new DefaultShortDisplayConverter();

    @Test
    public void testNonNullDataToDisplay() {
        Assert.assertEquals("123",
                this.shortConverter.canonicalToDisplayValue(Short.valueOf("123")));
    }

    @Test
    public void testNullDataToDisplay() {
        Assert.assertEquals(null, this.shortConverter.canonicalToDisplayValue(null));
    }

    @Test
    public void testNonNullDisplayToData() {
        Assert.assertEquals(Short.valueOf("123"),
                this.shortConverter.displayToCanonicalValue("123"));
    }

    @Test
    public void testNullDisplayToData() {
        Assert.assertEquals(null, this.shortConverter.displayToCanonicalValue(""));
    }

    @Test(expected = ConversionFailedException.class)
    public void testConversionException() {
        this.shortConverter.displayToCanonicalValue("abc");
    }

    @Test(expected = ConversionFailedException.class)
    public void testConversionExceptionTooBig() {
        short test = (Short) this.shortConverter.displayToCanonicalValue("32768");
        System.out.println(test);
    }
}
