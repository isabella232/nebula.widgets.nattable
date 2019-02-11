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
package org.eclipse.nebula.widgets.nattable.group.performance.command;

import org.eclipse.nebula.widgets.nattable.command.AbstractLayerCommandHandler;
import org.eclipse.nebula.widgets.nattable.group.performance.ColumnGroupHeaderLayer;

/**
 * Command handler for the {@link ColumnGroupReorderCommand}.
 *
 * @since 1.6
 */
public class ColumnGroupReorderCommandHandler extends AbstractLayerCommandHandler<ColumnGroupReorderCommand> {

    private final ColumnGroupHeaderLayer columnGroupHeaderLayer;

    public ColumnGroupReorderCommandHandler(ColumnGroupHeaderLayer columnGroupHeaderLayer) {
        this.columnGroupHeaderLayer = columnGroupHeaderLayer;
    }

    @Override
    protected boolean doCommand(ColumnGroupReorderCommand command) {
        int level = command.getLevel();
        int fromColumnPosition = command.getFromColumnPosition();
        int toColumnPosition = command.getToColumnPosition();

        if (!command.isReorderToLeftEdge()) {
            // needed to set the reorderToLeftEdge parameter correct on creating
            // the MultiColumnReorderCommand
            toColumnPosition++;
        }

        return this.columnGroupHeaderLayer.reorderColumnGroup(level, fromColumnPosition, toColumnPosition);
    }

    @Override
    public Class<ColumnGroupReorderCommand> getCommandClass() {
        return ColumnGroupReorderCommand.class;
    }

}
