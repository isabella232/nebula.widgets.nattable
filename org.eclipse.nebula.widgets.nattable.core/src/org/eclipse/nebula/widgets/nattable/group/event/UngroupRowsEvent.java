/*******************************************************************************
 * Copyright (c) 2019 Dirk Fauth.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Dirk Fauth <dirk.fauth@googlemail.com> - initial API and implementation
 ******************************************************************************/
package org.eclipse.nebula.widgets.nattable.group.event;

import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.layer.event.VisualRefreshEvent;

/**
 * Event to inform about ungrouping in a row group.
 *
 * @since 1.6
 */
public class UngroupRowsEvent extends VisualRefreshEvent {

    public UngroupRowsEvent(ILayer layer) {
        super(layer);
    }

    protected UngroupRowsEvent(UngroupRowsEvent event) {
        super(event);
    }

    @Override
    public UngroupRowsEvent cloneEvent() {
        return new UngroupRowsEvent(this);
    }

}
