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
package org.eclipse.nebula.widgets.nattable.group.performance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.nebula.widgets.nattable.data.ExtendedReflectiveColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.data.IColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.data.ListDataProvider;
import org.eclipse.nebula.widgets.nattable.dataset.person.ExtendedPersonWithAddress;
import org.eclipse.nebula.widgets.nattable.dataset.person.PersonService;
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
import org.eclipse.nebula.widgets.nattable.grid.command.ClientAreaResizeCommand;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultColumnHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultCornerDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultRowHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.layer.ColumnHeaderLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.CornerLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.DefaultColumnHeaderDataLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.DefaultRowHeaderDataLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.GridLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.RowHeaderLayer;
import org.eclipse.nebula.widgets.nattable.group.performance.GroupModel.Group;
import org.eclipse.nebula.widgets.nattable.group.performance.command.ColumnGroupReorderCommand;
import org.eclipse.nebula.widgets.nattable.group.performance.command.ColumnGroupReorderEndCommand;
import org.eclipse.nebula.widgets.nattable.group.performance.command.ColumnGroupReorderStartCommand;
import org.eclipse.nebula.widgets.nattable.group.performance.config.DefaultColumnGroupHeaderLayerConfiguration;
import org.eclipse.nebula.widgets.nattable.hideshow.ColumnHideShowLayer;
import org.eclipse.nebula.widgets.nattable.hideshow.command.ColumnHideCommand;
import org.eclipse.nebula.widgets.nattable.hideshow.command.MultiColumnHideCommand;
import org.eclipse.nebula.widgets.nattable.hideshow.command.ShowAllColumnsCommand;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.layer.LabelStack;
import org.eclipse.nebula.widgets.nattable.layer.cell.IConfigLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.nebula.widgets.nattable.reorder.ColumnReorderLayer;
import org.eclipse.nebula.widgets.nattable.reorder.command.ColumnReorderCommand;
import org.eclipse.nebula.widgets.nattable.reorder.command.ColumnReorderEndCommand;
import org.eclipse.nebula.widgets.nattable.reorder.command.ColumnReorderStartCommand;
import org.eclipse.nebula.widgets.nattable.reorder.command.MultiColumnReorderCommand;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.util.IClientAreaProvider;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;
import org.eclipse.nebula.widgets.nattable.viewport.command.ShowColumnInViewportCommand;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Before;
import org.junit.Test;

public class ColumnGroupHeaderLayerTest {

    GroupModel groupModel;
    ColumnGroupHeaderLayer columnGroupHeaderLayer;
    ColumnGroupExpandCollapseLayer columnGroupExpandCollapseLayer;
    SelectionLayer selectionLayer;
    GridLayer gridLayer;

    @Before
    public void setup() {
        String[] propertyNames = {
                "firstName", "lastName", "gender", "married",
                "address.street", "address.housenumber", "address.postalCode", "address.city",
                "age", "birthday", "money",
                "description", "favouriteFood", "favouriteDrinks" };

        // mapping from property to label, needed for column header labels
        Map<String, String> propertyToLabelMap = new HashMap<>();
        propertyToLabelMap.put("firstName", "Firstname");
        propertyToLabelMap.put("lastName", "Lastname");
        propertyToLabelMap.put("gender", "Gender");
        propertyToLabelMap.put("married", "Married");
        propertyToLabelMap.put("address.street", "Street");
        propertyToLabelMap.put("address.housenumber", "Housenumber");
        propertyToLabelMap.put("address.postalCode", "Postalcode");
        propertyToLabelMap.put("address.city", "City");
        propertyToLabelMap.put("age", "Age");
        propertyToLabelMap.put("birthday", "Birthday");
        propertyToLabelMap.put("money", "Money");
        propertyToLabelMap.put("description", "Description");
        propertyToLabelMap.put("favouriteFood", "Food");
        propertyToLabelMap.put("favouriteDrinks", "Drinks");

        IColumnPropertyAccessor<ExtendedPersonWithAddress> columnPropertyAccessor =
                new ExtendedReflectiveColumnPropertyAccessor<>(propertyNames);

        IDataProvider bodyDataProvider =
                new ListDataProvider<>(
                        PersonService.getExtendedPersonsWithAddress(10),
                        columnPropertyAccessor);
        DataLayer bodyDataLayer = new DataLayer(bodyDataProvider);
        ColumnReorderLayer columnReorderLayer = new ColumnReorderLayer(bodyDataLayer);
        ColumnHideShowLayer columnHideShowLayer = new ColumnHideShowLayer(columnReorderLayer);

        this.columnGroupExpandCollapseLayer = new ColumnGroupExpandCollapseLayer(columnHideShowLayer);

        this.selectionLayer = new SelectionLayer(this.columnGroupExpandCollapseLayer);
        ViewportLayer viewportLayer = new ViewportLayer(this.selectionLayer);

        // build the column header layer
        IDataProvider columnHeaderDataProvider = new DefaultColumnHeaderDataProvider(propertyNames, propertyToLabelMap);
        DataLayer columnHeaderDataLayer = new DefaultColumnHeaderDataLayer(columnHeaderDataProvider);
        ColumnHeaderLayer columnHeaderLayer = new ColumnHeaderLayer(columnHeaderDataLayer, viewportLayer, this.selectionLayer);
        this.columnGroupHeaderLayer = new ColumnGroupHeaderLayer(columnHeaderLayer, this.selectionLayer);

        this.groupModel = this.columnGroupHeaderLayer.getGroupModel();

        // configure the column groups
        this.columnGroupHeaderLayer.addGroup("Person", 0, 4);
        this.columnGroupHeaderLayer.addGroup("Address", 4, 4);
        this.columnGroupHeaderLayer.addGroup("Facts", 8, 3);
        this.columnGroupHeaderLayer.addGroup("Personal", 11, 3);

        // build the row header layer
        IDataProvider rowHeaderDataProvider = new DefaultRowHeaderDataProvider(bodyDataProvider);
        DataLayer rowHeaderDataLayer = new DefaultRowHeaderDataLayer(rowHeaderDataProvider);
        ILayer rowHeaderLayer = new RowHeaderLayer(rowHeaderDataLayer, viewportLayer, this.selectionLayer);

        // build the corner layer
        IDataProvider cornerDataProvider = new DefaultCornerDataProvider(columnHeaderDataProvider, rowHeaderDataProvider);
        DataLayer cornerDataLayer = new DataLayer(cornerDataProvider);
        ILayer cornerLayer = new CornerLayer(cornerDataLayer, rowHeaderLayer, this.columnGroupHeaderLayer);

        // build the grid layer
        this.gridLayer = new GridLayer(viewportLayer, this.columnGroupHeaderLayer, rowHeaderLayer, cornerLayer);

        // configure the visible area, needed for tests in scrolled state
        this.gridLayer.setClientAreaProvider(new IClientAreaProvider() {

            @Override
            public Rectangle getClientArea() {
                // 10 columns + row header should be visible
                return new Rectangle(0, 0, 1010, 250);
            }

        });
        this.gridLayer.doCommand(new ClientAreaResizeCommand(new Shell(Display.getDefault(), SWT.V_SCROLL | SWT.H_SCROLL)));

        assertEquals(1, this.columnGroupHeaderLayer.getLevelCount());
        verifyCleanState();
    }

    private void verifyCleanState() {
        // nothing hidden below the SelectionLayer
        assertEquals(14, this.selectionLayer.getColumnCount());

        for (int column = 0; column < this.columnGroupHeaderLayer.getColumnCount(); column++) {
            assertTrue(this.columnGroupHeaderLayer.isPartOfAGroup(column));
            assertFalse(this.columnGroupHeaderLayer.isPartOfAnUnbreakableGroup(column));
        }

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(400, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
        assertEquals(4, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(400, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(400, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(8, 0);
        assertEquals(8, cell.getOriginColumnPosition());
        assertEquals(3, cell.getColumnSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(800, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(300, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        // this cell is not visible because of the client area
        cell = this.columnGroupHeaderLayer.getCellByPosition(11, 0);
        assertEquals(11, cell.getOriginColumnPosition());
        assertEquals(3, cell.getColumnSpan());
        assertEquals("Personal", cell.getDataValue());
        assertEquals(-1, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());

        Group group2 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());

        Group group3 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(8, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());

        Group group4 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(11, group4.getStartIndex());
        assertEquals(11, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());
    }

    @Test
    public void shouldRenderColumnGroups() {
        assertEquals(11, this.gridLayer.getColumnCount());
        assertEquals(12, this.gridLayer.getRowCount());

        // increase the client area to show all columns
        this.gridLayer.setClientAreaProvider(new IClientAreaProvider() {

            @Override
            public Rectangle getClientArea() {
                return new Rectangle(0, 0, 1600, 250);
            }

        });
        this.gridLayer.doCommand(new ClientAreaResizeCommand(new Shell(Display.getDefault(), SWT.V_SCROLL | SWT.H_SCROLL)));

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(400, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
        assertEquals(4, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(400, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(400, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(8, 0);
        assertEquals(8, cell.getOriginColumnPosition());
        assertEquals(3, cell.getColumnSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(800, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(300, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(11, 0);
        assertEquals(11, cell.getOriginColumnPosition());
        assertEquals(3, cell.getColumnSpan());
        assertEquals("Personal", cell.getDataValue());
        assertEquals(1100, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(300, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);
    }

    @Test
    public void shouldReturnSameCellForDifferentColumnPositions() {
        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(cell, this.columnGroupHeaderLayer.getCellByPosition(1, 0));
        assertEquals(cell, this.columnGroupHeaderLayer.getCellByPosition(2, 0));
        assertEquals(cell, this.columnGroupHeaderLayer.getCellByPosition(3, 0));

        // the next cell is the start of the next column group
        assertFalse(cell.equals(this.columnGroupHeaderLayer.getCellByPosition(4, 0)));
    }

    @Test
    public void shouldRenderGroupInScrolledState() {
        assertEquals(0, this.gridLayer.getBodyLayer().getColumnIndexByPosition(0));

        // scroll
        this.gridLayer.doCommand(new ShowColumnInViewportCommand(11));

        assertEquals(2, this.gridLayer.getBodyLayer().getColumnIndexByPosition(0));

        int visibleStartPosition = this.groupModel.getGroupByPosition(0).getVisibleStartPosition();
        assertEquals(0, visibleStartPosition);
        assertEquals(2, this.columnGroupHeaderLayer.getColumnIndexByPosition(visibleStartPosition));

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(-2, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnSpan());
        assertEquals(new Rectangle(-230, 0, 400, 20), cell.getBounds());
    }

    @Test
    public void shouldCheckIfPartOfGroup() {
        // remove last column from first group
        this.columnGroupHeaderLayer.removePositionsFromGroup(0, 3);

        // set second group as unbreakable
        this.columnGroupHeaderLayer.setGroupUnbreakable(4, true);

        for (int column = 0; column < this.columnGroupHeaderLayer.getColumnCount(); column++) {

            // check part of a group
            if (column == 3) {
                assertFalse(this.columnGroupHeaderLayer.isPartOfAGroup(column));
            } else {
                assertTrue(this.columnGroupHeaderLayer.isPartOfAGroup(column));
            }

            // check part of an unbreakable group
            if (column >= 4 && column < 8) {
                assertTrue(this.columnGroupHeaderLayer.isPartOfAnUnbreakableGroup(column));
            } else {
                assertFalse(this.columnGroupHeaderLayer.isPartOfAnUnbreakableGroup(column));
            }
        }

    }

    @Test
    public void shouldRemoveLastColumnFromGroup() {
        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(400, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        // remove last column from first group
        this.columnGroupHeaderLayer.removePositionsFromGroup(0, 3);

        cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(3, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(300, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(3, 0);
        assertEquals(3, cell.getOriginColumnPosition());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(2, cell.getRowSpan());
        assertEquals("Married", cell.getDataValue());
        assertEquals(300, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(100, cell.getBounds().width);
        assertEquals(40, cell.getBounds().height);
    }

    @Test
    public void shouldRemoveFirstColumnFromGroup() {
        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(400, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        // remove first column from first group
        this.columnGroupHeaderLayer.removePositionsFromGroup(0, 0);

        cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(2, cell.getRowSpan());
        assertEquals("Firstname", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(100, cell.getBounds().width);
        assertEquals(40, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(1, 0);
        assertEquals(1, cell.getOriginColumnPosition());
        assertEquals(3, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(100, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(300, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);
    }

    @Test
    public void shouldRemoveMiddleColumnFromGroup() {
        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(400, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        // remove middle column from first group
        this.columnGroupHeaderLayer.removePositionsFromGroup(0, 1);

        // the result is the same as removing the last column in a group, as it
        // is not possible to split a column group by removing a middle group
        cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(3, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(300, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(3, 0);
        assertEquals(3, cell.getOriginColumnPosition());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(2, cell.getRowSpan());
        assertEquals("Married", cell.getDataValue());
        assertEquals(300, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(100, cell.getBounds().width);
        assertEquals(40, cell.getBounds().height);
    }

    @Test
    public void shouldAddColumnToEndOfGroup() {
        // remove last column from first group
        this.columnGroupHeaderLayer.removePositionsFromGroup(0, 3);

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(3, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(300, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(3, 0);
        assertEquals(3, cell.getOriginColumnPosition());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(2, cell.getRowSpan());
        assertEquals("Married", cell.getDataValue());
        assertEquals(300, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(100, cell.getBounds().width);
        assertEquals(40, cell.getBounds().height);

        // add the column back again
        this.columnGroupHeaderLayer.addPositionsToGroup(0, 0, 3);

        cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(400, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);
    }

    @Test
    public void shouldAddColumnAtStartOfGroup() {
        // remove last column from first group
        this.columnGroupHeaderLayer.removePositionsFromGroup(0, 3);

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(3, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(300, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(3, 0);
        assertEquals(3, cell.getOriginColumnPosition());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(2, cell.getRowSpan());
        assertEquals("Married", cell.getDataValue());
        assertEquals(300, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(100, cell.getBounds().width);
        assertEquals(40, cell.getBounds().height);

        // add the column as first column to the second group
        this.columnGroupHeaderLayer.addPositionsToGroup(0, 4, 3);

        cell = this.columnGroupHeaderLayer.getCellByPosition(3, 0);
        assertEquals(3, cell.getOriginColumnPosition());
        assertEquals(5, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(300, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(500, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);
    }

    @Test
    public void shouldHideColumnInMiddleOfGroup() {
        if (this.gridLayer.doCommand(new ColumnHideCommand(this.gridLayer, 3))) {
            ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginColumnPosition());
            assertEquals(3, cell.getColumnSpan());
            assertEquals("Person", cell.getDataValue());
            assertEquals(0, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(300, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(4, group1.getOriginalSpan());
            assertEquals(3, group1.getVisibleSpan());

            Group group2 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
            assertEquals(4, group2.getStartIndex());
            assertEquals(4, group2.getVisibleStartIndex());
            assertEquals(3, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(4, group2.getVisibleSpan());

            Group group3 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(7);
            assertEquals(8, group3.getStartIndex());
            assertEquals(8, group3.getVisibleStartIndex());
            assertEquals(7, group3.getVisibleStartPosition());
            assertEquals(3, group3.getOriginalSpan());
            assertEquals(3, group3.getVisibleSpan());

            Group group4 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(10);
            assertEquals(11, group4.getStartIndex());
            assertEquals(11, group4.getVisibleStartIndex());
            assertEquals(10, group4.getVisibleStartPosition());
            assertEquals(3, group4.getOriginalSpan());
            assertEquals(3, group4.getVisibleSpan());
        } else {
            fail("Column not hidden");
        }

        // show again
        if (this.gridLayer.doCommand(new ShowAllColumnsCommand())) {
            verifyCleanState();
        } else {
            fail("Columns not shown again");
        }
    }

    @Test
    public void shouldHideLastColumnInGroup() {
        if (this.gridLayer.doCommand(new ColumnHideCommand(this.gridLayer, 4))) {
            ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginColumnPosition());
            assertEquals(3, cell.getColumnSpan());
            assertEquals("Person", cell.getDataValue());
            assertEquals(0, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(300, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(4, group1.getOriginalSpan());
            assertEquals(3, group1.getVisibleSpan());

            Group group2 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
            assertEquals(4, group2.getStartIndex());
            assertEquals(4, group2.getVisibleStartIndex());
            assertEquals(3, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(4, group2.getVisibleSpan());

            Group group3 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(7);
            assertEquals(8, group3.getStartIndex());
            assertEquals(8, group3.getVisibleStartIndex());
            assertEquals(7, group3.getVisibleStartPosition());
            assertEquals(3, group3.getOriginalSpan());
            assertEquals(3, group3.getVisibleSpan());

            Group group4 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(10);
            assertEquals(11, group4.getStartIndex());
            assertEquals(11, group4.getVisibleStartIndex());
            assertEquals(10, group4.getVisibleStartPosition());
            assertEquals(3, group4.getOriginalSpan());
            assertEquals(3, group4.getVisibleSpan());
        } else {
            fail("Column not hidden");
        }

        // show again
        if (this.gridLayer.doCommand(new ShowAllColumnsCommand())) {
            verifyCleanState();
        } else {
            fail("Columns not shown again");
        }
    }

    @Test
    public void shouldHideFirstColumnInGroup() {
        if (this.gridLayer.doCommand(new ColumnHideCommand(this.gridLayer, 5))) {
            ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
            assertEquals(4, cell.getOriginColumnPosition());
            assertEquals(3, cell.getColumnSpan());
            assertEquals("Address", cell.getDataValue());
            assertEquals(400, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(300, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(4, group1.getOriginalSpan());
            assertEquals(4, group1.getVisibleSpan());

            Group group2 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
            assertEquals(4, group2.getStartIndex());
            assertEquals(5, group2.getVisibleStartIndex());
            assertEquals(4, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(3, group2.getVisibleSpan());

            Group group3 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(7);
            assertEquals(8, group3.getStartIndex());
            assertEquals(8, group3.getVisibleStartIndex());
            assertEquals(7, group3.getVisibleStartPosition());
            assertEquals(3, group3.getOriginalSpan());
            assertEquals(3, group3.getVisibleSpan());

            Group group4 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(10);
            assertEquals(11, group4.getStartIndex());
            assertEquals(11, group4.getVisibleStartIndex());
            assertEquals(10, group4.getVisibleStartPosition());
            assertEquals(3, group4.getOriginalSpan());
            assertEquals(3, group4.getVisibleSpan());
        } else {
            fail("Column not hidden");
        }

        // show again
        if (this.gridLayer.doCommand(new ShowAllColumnsCommand())) {
            verifyCleanState();
        } else {
            fail("Columns not shown again");
        }
    }

    @Test
    public void shouldHideMultipleMiddleColumns() {
        if (this.gridLayer.doCommand(new MultiColumnHideCommand(this.gridLayer, 3, 6, 10))) {
            ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginColumnPosition());
            assertEquals(3, cell.getColumnSpan());
            assertEquals("Person", cell.getDataValue());
            assertEquals(0, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(300, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            cell = this.columnGroupHeaderLayer.getCellByPosition(3, 0);
            assertEquals(3, cell.getOriginColumnPosition());
            assertEquals(3, cell.getColumnSpan());
            assertEquals("Address", cell.getDataValue());
            assertEquals(300, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(300, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            cell = this.columnGroupHeaderLayer.getCellByPosition(6, 0);
            assertEquals(6, cell.getOriginColumnPosition());
            assertEquals(2, cell.getColumnSpan());
            assertEquals("Facts", cell.getDataValue());
            assertEquals(600, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(200, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            cell = this.columnGroupHeaderLayer.getCellByPosition(8, 0);
            assertEquals(8, cell.getOriginColumnPosition());
            assertEquals(3, cell.getColumnSpan());
            assertEquals("Personal", cell.getDataValue());
            assertEquals(800, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(300, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(4, group1.getOriginalSpan());
            assertEquals(3, group1.getVisibleSpan());

            Group group2 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
            assertEquals(4, group2.getStartIndex());
            assertEquals(4, group2.getVisibleStartIndex());
            assertEquals(3, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(3, group2.getVisibleSpan());

            Group group3 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(6);
            assertEquals(8, group3.getStartIndex());
            assertEquals(8, group3.getVisibleStartIndex());
            assertEquals(6, group3.getVisibleStartPosition());
            assertEquals(3, group3.getOriginalSpan());
            assertEquals(2, group3.getVisibleSpan());

            Group group4 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
            assertEquals(11, group4.getStartIndex());
            assertEquals(11, group4.getVisibleStartIndex());
            assertEquals(8, group4.getVisibleStartPosition());
            assertEquals(3, group4.getOriginalSpan());
            assertEquals(3, group4.getVisibleSpan());
        } else {
            fail("Column not hidden");
        }

        // show again
        if (this.gridLayer.doCommand(new ShowAllColumnsCommand())) {
            verifyCleanState();
        } else {
            fail("Columns not shown again");
        }
    }

    @Test
    public void shouldHideMultipleFirstColumns() {
        if (this.gridLayer.doCommand(new MultiColumnHideCommand(this.gridLayer, 1, 5, 9))) {
            ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginColumnPosition());
            assertEquals(3, cell.getColumnSpan());
            assertEquals("Person", cell.getDataValue());
            assertEquals(0, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(300, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            cell = this.columnGroupHeaderLayer.getCellByPosition(3, 0);
            assertEquals(3, cell.getOriginColumnPosition());
            assertEquals(3, cell.getColumnSpan());
            assertEquals("Address", cell.getDataValue());
            assertEquals(300, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(300, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            cell = this.columnGroupHeaderLayer.getCellByPosition(6, 0);
            assertEquals(6, cell.getOriginColumnPosition());
            assertEquals(2, cell.getColumnSpan());
            assertEquals("Facts", cell.getDataValue());
            assertEquals(600, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(200, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            cell = this.columnGroupHeaderLayer.getCellByPosition(8, 0);
            assertEquals(8, cell.getOriginColumnPosition());
            assertEquals(3, cell.getColumnSpan());
            assertEquals("Personal", cell.getDataValue());
            assertEquals(800, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(300, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
            assertEquals(0, group1.getStartIndex());
            assertEquals(1, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(4, group1.getOriginalSpan());
            assertEquals(3, group1.getVisibleSpan());

            Group group2 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
            assertEquals(4, group2.getStartIndex());
            assertEquals(5, group2.getVisibleStartIndex());
            assertEquals(3, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(3, group2.getVisibleSpan());

            Group group3 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(6);
            assertEquals(8, group3.getStartIndex());
            assertEquals(9, group3.getVisibleStartIndex());
            assertEquals(6, group3.getVisibleStartPosition());
            assertEquals(3, group3.getOriginalSpan());
            assertEquals(2, group3.getVisibleSpan());

            Group group4 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
            assertEquals(11, group4.getStartIndex());
            assertEquals(11, group4.getVisibleStartIndex());
            assertEquals(8, group4.getVisibleStartPosition());
            assertEquals(3, group4.getOriginalSpan());
            assertEquals(3, group4.getVisibleSpan());
        } else {
            fail("Column not hidden");
        }

        // show again
        if (this.gridLayer.doCommand(new ShowAllColumnsCommand())) {
            verifyCleanState();
        } else {
            fail("Columns not shown again");
        }
    }

    @Test
    public void shouldHideMultipleLastColumns() {
        // trigger the command on the SelectionLayer as we hide a column that is
        // not visible which would be blocked by command handling through the
        // ViewportLayer
        if (this.selectionLayer.doCommand(new MultiColumnHideCommand(this.selectionLayer, 3, 7, 10))) {
            ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginColumnPosition());
            assertEquals(3, cell.getColumnSpan());
            assertEquals("Person", cell.getDataValue());
            assertEquals(0, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(300, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            cell = this.columnGroupHeaderLayer.getCellByPosition(3, 0);
            assertEquals(3, cell.getOriginColumnPosition());
            assertEquals(3, cell.getColumnSpan());
            assertEquals("Address", cell.getDataValue());
            assertEquals(300, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(300, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            cell = this.columnGroupHeaderLayer.getCellByPosition(6, 0);
            assertEquals(6, cell.getOriginColumnPosition());
            assertEquals(2, cell.getColumnSpan());
            assertEquals("Facts", cell.getDataValue());
            assertEquals(600, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(200, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            cell = this.columnGroupHeaderLayer.getCellByPosition(8, 0);
            assertEquals(8, cell.getOriginColumnPosition());
            assertEquals(3, cell.getColumnSpan());
            assertEquals("Personal", cell.getDataValue());
            assertEquals(800, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(300, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(4, group1.getOriginalSpan());
            assertEquals(3, group1.getVisibleSpan());

            Group group2 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
            assertEquals(4, group2.getStartIndex());
            assertEquals(4, group2.getVisibleStartIndex());
            assertEquals(3, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(3, group2.getVisibleSpan());

            Group group3 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(6);
            assertEquals(8, group3.getStartIndex());
            assertEquals(8, group3.getVisibleStartIndex());
            assertEquals(6, group3.getVisibleStartPosition());
            assertEquals(3, group3.getOriginalSpan());
            assertEquals(2, group3.getVisibleSpan());

            Group group4 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
            assertEquals(11, group4.getStartIndex());
            assertEquals(11, group4.getVisibleStartIndex());
            assertEquals(8, group4.getVisibleStartPosition());
            assertEquals(3, group4.getOriginalSpan());
            assertEquals(3, group4.getVisibleSpan());
        } else {
            fail("Column not hidden");
        }

        // show again
        if (this.gridLayer.doCommand(new ShowAllColumnsCommand())) {
            verifyCleanState();
        } else {
            fail("Columns not shown again");
        }
    }

    @Test
    public void shouldHideMultipleMixedColumns() {
        // last/first/middle
        if (this.selectionLayer.doCommand(new MultiColumnHideCommand(this.selectionLayer, 3, 4, 9))) {
            ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginColumnPosition());
            assertEquals(3, cell.getColumnSpan());
            assertEquals("Person", cell.getDataValue());
            assertEquals(0, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(300, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            cell = this.columnGroupHeaderLayer.getCellByPosition(3, 0);
            assertEquals(3, cell.getOriginColumnPosition());
            assertEquals(3, cell.getColumnSpan());
            assertEquals("Address", cell.getDataValue());
            assertEquals(300, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(300, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            cell = this.columnGroupHeaderLayer.getCellByPosition(6, 0);
            assertEquals(6, cell.getOriginColumnPosition());
            assertEquals(2, cell.getColumnSpan());
            assertEquals("Facts", cell.getDataValue());
            assertEquals(600, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(200, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            cell = this.columnGroupHeaderLayer.getCellByPosition(8, 0);
            assertEquals(8, cell.getOriginColumnPosition());
            assertEquals(3, cell.getColumnSpan());
            assertEquals("Personal", cell.getDataValue());
            assertEquals(800, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(300, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(4, group1.getOriginalSpan());
            assertEquals(3, group1.getVisibleSpan());

            Group group2 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
            assertEquals(4, group2.getStartIndex());
            assertEquals(5, group2.getVisibleStartIndex());
            assertEquals(3, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(3, group2.getVisibleSpan());

            Group group3 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(6);
            assertEquals(8, group3.getStartIndex());
            assertEquals(8, group3.getVisibleStartIndex());
            assertEquals(6, group3.getVisibleStartPosition());
            assertEquals(3, group3.getOriginalSpan());
            assertEquals(2, group3.getVisibleSpan());

            Group group4 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
            assertEquals(11, group4.getStartIndex());
            assertEquals(11, group4.getVisibleStartIndex());
            assertEquals(8, group4.getVisibleStartPosition());
            assertEquals(3, group4.getOriginalSpan());
            assertEquals(3, group4.getVisibleSpan());
        } else {
            fail("Column not hidden");
        }

        // show again
        if (this.gridLayer.doCommand(new ShowAllColumnsCommand())) {
            verifyCleanState();
        } else {
            fail("Columns not shown again");
        }
    }

    @Test
    public void shouldHideMultipleColumnsInOneGroup() {
        // first two in second group
        if (this.selectionLayer.doCommand(new MultiColumnHideCommand(this.selectionLayer, 4, 5))) {
            ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginColumnPosition());
            assertEquals(4, cell.getColumnSpan());
            assertEquals("Person", cell.getDataValue());
            assertEquals(0, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(400, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
            assertEquals(4, cell.getOriginColumnPosition());
            assertEquals(2, cell.getColumnSpan());
            assertEquals("Address", cell.getDataValue());
            assertEquals(400, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(200, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            cell = this.columnGroupHeaderLayer.getCellByPosition(6, 0);
            assertEquals(6, cell.getOriginColumnPosition());
            assertEquals(3, cell.getColumnSpan());
            assertEquals("Facts", cell.getDataValue());
            assertEquals(600, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(300, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            cell = this.columnGroupHeaderLayer.getCellByPosition(9, 0);
            assertEquals(9, cell.getOriginColumnPosition());
            assertEquals(3, cell.getColumnSpan());
            assertEquals("Personal", cell.getDataValue());
            assertEquals(900, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(300, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(4, group1.getOriginalSpan());
            assertEquals(4, group1.getVisibleSpan());

            Group group2 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
            assertEquals(4, group2.getStartIndex());
            assertEquals(6, group2.getVisibleStartIndex());
            assertEquals(4, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(2, group2.getVisibleSpan());

            Group group3 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(6);
            assertEquals(8, group3.getStartIndex());
            assertEquals(8, group3.getVisibleStartIndex());
            assertEquals(6, group3.getVisibleStartPosition());
            assertEquals(3, group3.getOriginalSpan());
            assertEquals(3, group3.getVisibleSpan());

            Group group4 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(9);
            assertEquals(11, group4.getStartIndex());
            assertEquals(11, group4.getVisibleStartIndex());
            assertEquals(9, group4.getVisibleStartPosition());
            assertEquals(3, group4.getOriginalSpan());
            assertEquals(3, group4.getVisibleSpan());
        } else {
            fail("Column not hidden");
        }

        // show again
        if (this.gridLayer.doCommand(new ShowAllColumnsCommand())) {
            verifyCleanState();
        } else {
            fail("Columns not shown again");
        }
    }

    @Test
    public void shouldHideAllColumnsInOneGroup() {
        // second group
        if (this.selectionLayer.doCommand(new MultiColumnHideCommand(this.selectionLayer, 4, 5, 6, 7))) {
            ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginColumnPosition());
            assertEquals(4, cell.getColumnSpan());
            assertEquals("Person", cell.getDataValue());
            assertEquals(0, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(400, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
            assertEquals(4, cell.getOriginColumnPosition());
            assertEquals(3, cell.getColumnSpan());
            assertEquals("Facts", cell.getDataValue());
            assertEquals(400, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(300, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            cell = this.columnGroupHeaderLayer.getCellByPosition(7, 0);
            assertEquals(7, cell.getOriginColumnPosition());
            assertEquals(3, cell.getColumnSpan());
            assertEquals("Personal", cell.getDataValue());
            assertEquals(700, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(300, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(4, group1.getOriginalSpan());
            assertEquals(4, group1.getVisibleSpan());

            Group group3 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(6);
            assertEquals(8, group3.getStartIndex());
            assertEquals(8, group3.getVisibleStartIndex());
            assertEquals(4, group3.getVisibleStartPosition());
            assertEquals(3, group3.getOriginalSpan());
            assertEquals(3, group3.getVisibleSpan());

            Group group4 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
            assertEquals(11, group4.getStartIndex());
            assertEquals(11, group4.getVisibleStartIndex());
            assertEquals(7, group4.getVisibleStartPosition());
            assertEquals(3, group4.getOriginalSpan());
            assertEquals(3, group4.getVisibleSpan());

            // this group is not visible by column position, so we retrieve it
            // by name
            Group group2 = this.columnGroupHeaderLayer.getGroupModel().getGroupByName("Address");
            assertEquals(4, group2.getStartIndex());
            assertEquals(-1, group2.getVisibleStartIndex());
            assertEquals(-1, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(0, group2.getVisibleSpan());
        } else {
            fail("Column not hidden");
        }

        // show again
        if (this.gridLayer.doCommand(new ShowAllColumnsCommand())) {
            verifyCleanState();
        } else {
            fail("Columns not shown again");
        }
    }

    @Test
    public void shouldHideColumnBetweenGroups() {
        // remove last column from first group
        this.columnGroupHeaderLayer.removePositionsFromGroup(0, 3);

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(3, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(300, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(3, 0);
        assertEquals(3, cell.getOriginColumnPosition());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(2, cell.getRowSpan());
        assertEquals("Married", cell.getDataValue());
        assertEquals(300, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(100, cell.getBounds().width);
        assertEquals(40, cell.getBounds().height);

        // hide column
        if (this.selectionLayer.doCommand(new ColumnHideCommand(this.selectionLayer, 3))) {
            cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginColumnPosition());
            assertEquals(3, cell.getColumnSpan());
            assertEquals("Person", cell.getDataValue());
            assertEquals(0, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(300, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            cell = this.columnGroupHeaderLayer.getCellByPosition(3, 0);
            assertEquals(3, cell.getOriginColumnPosition());
            assertEquals(4, cell.getColumnSpan());
            assertEquals(1, cell.getRowSpan());
            assertEquals("Address", cell.getDataValue());
            assertEquals(300, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(400, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            Group group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
            assertEquals(4, group.getStartIndex());
            assertEquals(4, group.getVisibleStartIndex());
            assertEquals(3, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(4, group.getVisibleSpan());
        } else {
            fail("Column not hidden");
        }

        // show column again
        if (this.gridLayer.doCommand(new ShowAllColumnsCommand())) {
            cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginColumnPosition());
            assertEquals(3, cell.getColumnSpan());
            assertEquals("Person", cell.getDataValue());
            assertEquals(0, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(300, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            cell = this.columnGroupHeaderLayer.getCellByPosition(3, 0);
            assertEquals(3, cell.getOriginColumnPosition());
            assertEquals(1, cell.getColumnSpan());
            assertEquals(2, cell.getRowSpan());
            assertEquals("Married", cell.getDataValue());
            assertEquals(300, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(100, cell.getBounds().width);
            assertEquals(40, cell.getBounds().height);

            Group group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(2);
            assertEquals(0, group.getStartIndex());
            assertEquals(0, group.getVisibleStartIndex());
            assertEquals(0, group.getVisibleStartPosition());
            assertEquals(3, group.getOriginalSpan());
            assertEquals(3, group.getVisibleSpan());

            assertNull(this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(3));

            group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
            assertEquals(4, group.getStartIndex());
            assertEquals(4, group.getVisibleStartIndex());
            assertEquals(4, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(4, group.getVisibleSpan());
        } else {
            fail("Columns not shown again");
        }
    }

    @Test
    public void shouldCollapseExpandGroup() {
        assertEquals(14, this.columnGroupExpandCollapseLayer.getColumnCount());

        // collapse group with no static indexes
        this.columnGroupHeaderLayer.collapseGroup(0);

        assertEquals(11, this.columnGroupExpandCollapseLayer.getColumnCount());

        Group group = this.columnGroupHeaderLayer.getGroupByPosition(0);
        assertTrue(group.isCollapsed());
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());

        // expand group with no static indexes
        this.columnGroupHeaderLayer.expandGroup(0);

        assertEquals(14, this.columnGroupExpandCollapseLayer.getColumnCount());

        group = this.columnGroupHeaderLayer.getGroupByPosition(0);
        assertFalse(group.isCollapsed());
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(4, group.getVisibleSpan());

        cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(4, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
    }

    @Test
    public void shouldCollapseGroupWithStaticColumns() {
        this.columnGroupHeaderLayer.addStaticColumnIndexesToGroup(0, 4, 6, 7);

        assertEquals(14, this.columnGroupExpandCollapseLayer.getColumnCount());

        // collapse group with static indexes
        this.columnGroupHeaderLayer.collapseGroup(4);

        assertEquals(12, this.columnGroupExpandCollapseLayer.getColumnCount());

        Group group = this.columnGroupHeaderLayer.getGroupByPosition(4);
        assertTrue(group.isCollapsed());
        assertEquals(4, group.getStartIndex());
        assertEquals(6, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(2, group.getVisibleSpan());

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
        assertEquals(4, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnPosition());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());

        // expand group with static indexes
        this.columnGroupHeaderLayer.expandGroup(4);

        assertEquals(14, this.columnGroupExpandCollapseLayer.getColumnCount());

        group = this.columnGroupHeaderLayer.getGroupByPosition(4);
        assertFalse(group.isCollapsed());
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(4, group.getVisibleSpan());

        cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
        assertEquals(4, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnPosition());
        assertEquals(4, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
    }

    @Test
    public void shouldShowFirstVisibleColumnOnCollapseWhenFirstColumnIsHidden() {
        assertEquals(14, this.columnGroupExpandCollapseLayer.getColumnCount());

        // hide first column in group
        if (this.gridLayer.doCommand(new ColumnHideCommand(this.gridLayer, 1))) {
            assertEquals(13, this.columnGroupExpandCollapseLayer.getColumnCount());

            // collapse group with no static indexes
            this.columnGroupHeaderLayer.collapseGroup(0);

            assertEquals(11, this.columnGroupExpandCollapseLayer.getColumnCount());

            Group group = this.columnGroupHeaderLayer.getGroupByPosition(0);
            assertTrue(group.isCollapsed());
            assertEquals(0, group.getStartIndex());
            assertEquals(1, group.getVisibleStartIndex());
            assertEquals(0, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(1, group.getVisibleSpan());

            ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginColumnPosition());
            assertEquals(0, cell.getColumnPosition());
            assertEquals(1, cell.getColumnIndex());
            assertEquals(1, cell.getColumnSpan());
            assertEquals(1, cell.getRowSpan());

            // expand group with no static indexes
            this.columnGroupHeaderLayer.expandGroup(0);

            assertEquals(13, this.columnGroupExpandCollapseLayer.getColumnCount());

            group = this.columnGroupHeaderLayer.getGroupByPosition(0);
            assertFalse(group.isCollapsed());
            assertEquals(0, group.getStartIndex());
            assertEquals(1, group.getVisibleStartIndex());
            assertEquals(0, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(3, group.getVisibleSpan());

            cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginColumnPosition());
            assertEquals(0, cell.getColumnPosition());
            assertEquals(1, cell.getColumnIndex());
            assertEquals(3, cell.getColumnSpan());
            assertEquals(1, cell.getRowSpan());
        } else {
            fail("Column not hidden");
        }

        // show all columns again
        if (this.gridLayer.doCommand(new ShowAllColumnsCommand())) {
            assertEquals(14, this.columnGroupExpandCollapseLayer.getColumnCount());

            Group group = this.columnGroupHeaderLayer.getGroupByPosition(0);
            assertFalse(group.isCollapsed());
            assertEquals(0, group.getStartIndex());
            assertEquals(0, group.getVisibleStartIndex());
            assertEquals(0, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(4, group.getVisibleSpan());

            ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginColumnPosition());
            assertEquals(0, cell.getColumnPosition());
            assertEquals(0, cell.getColumnIndex());
            assertEquals(4, cell.getColumnSpan());
            assertEquals(1, cell.getRowSpan());
        } else {
            fail("Columns not shown again");
        }
    }

    @Test
    public void shouldNotShowHiddenColumnInCollapsedGroup() {
        assertEquals(14, this.columnGroupExpandCollapseLayer.getColumnCount());

        // hide column in group
        if (this.gridLayer.doCommand(new ColumnHideCommand(this.gridLayer, 3))) {
            assertEquals(13, this.columnGroupExpandCollapseLayer.getColumnCount());

            // collapse group
            this.columnGroupHeaderLayer.collapseGroup(0);

            assertEquals(11, this.columnGroupExpandCollapseLayer.getColumnCount());

            Group group = this.columnGroupHeaderLayer.getGroupByPosition(0);
            assertTrue(group.isCollapsed());
            assertEquals(0, group.getStartIndex());
            assertEquals(0, group.getVisibleStartIndex());
            assertEquals(0, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(1, group.getVisibleSpan());

            ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginColumnPosition());
            assertEquals(0, cell.getColumnPosition());
            assertEquals(0, cell.getColumnIndex());
            assertEquals(1, cell.getColumnSpan());
            assertEquals(1, cell.getRowSpan());
        } else {
            fail("Column not hidden");
        }

        // show all columns again
        // collapsed columns should stay hidden
        if (this.gridLayer.doCommand(new ShowAllColumnsCommand())) {
            assertEquals(11, this.columnGroupExpandCollapseLayer.getColumnCount());

            Group group = this.columnGroupHeaderLayer.getGroupByPosition(0);
            assertTrue(group.isCollapsed());
            assertEquals(0, group.getStartIndex());
            assertEquals(0, group.getVisibleStartIndex());
            assertEquals(0, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(1, group.getVisibleSpan());

            ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginColumnPosition());
            assertEquals(0, cell.getColumnPosition());
            assertEquals(0, cell.getColumnIndex());
            assertEquals(1, cell.getColumnSpan());
            assertEquals(1, cell.getRowSpan());
        } else {
            fail("Columns not shown again");
        }

        // expand again to check that the group state is not changed
        this.columnGroupHeaderLayer.expandGroup(0);

        Group group = this.columnGroupHeaderLayer.getGroupByPosition(0);
        assertFalse(group.isCollapsed());
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(4, group.getVisibleSpan());

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(0, cell.getColumnIndex());
        assertEquals(4, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
    }

    @Test
    public void shouldNotShowHiddenFirstColumnInCollapsedGroup() {
        assertEquals(14, this.columnGroupExpandCollapseLayer.getColumnCount());

        // hide column in group
        if (this.selectionLayer.doCommand(new ColumnHideCommand(this.selectionLayer, 4))) {
            assertEquals(13, this.columnGroupExpandCollapseLayer.getColumnCount());

            // collapse group
            this.columnGroupHeaderLayer.collapseGroup(4);

            assertEquals(11, this.columnGroupExpandCollapseLayer.getColumnCount());

            Group group = this.columnGroupHeaderLayer.getGroupByPosition(4);
            assertTrue(group.isCollapsed());
            assertEquals(4, group.getStartIndex());
            assertEquals(5, group.getVisibleStartIndex());
            assertEquals(4, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(1, group.getVisibleSpan());

            ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
            assertEquals(4, cell.getOriginColumnPosition());
            assertEquals(4, cell.getColumnPosition());
            assertEquals(5, cell.getColumnIndex());
            assertEquals(1, cell.getColumnSpan());
            assertEquals(1, cell.getRowSpan());
        } else {
            fail("Column not hidden");
        }

        // show all columns again
        // collapsed columns should stay hidden
        if (this.gridLayer.doCommand(new ShowAllColumnsCommand())) {
            assertEquals(11, this.columnGroupExpandCollapseLayer.getColumnCount());

            Group group = this.columnGroupHeaderLayer.getGroupByPosition(4);
            assertTrue(group.isCollapsed());
            assertEquals(4, group.getStartIndex());
            assertEquals(4, group.getVisibleStartIndex());
            assertEquals(4, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(1, group.getVisibleSpan());

            ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
            assertEquals(4, cell.getOriginColumnPosition());
            assertEquals(4, cell.getColumnPosition());
            assertEquals(4, cell.getColumnIndex());
            assertEquals(1, cell.getColumnSpan());
            assertEquals(1, cell.getRowSpan());
        } else {
            fail("Columns not shown again");
        }

        // expand again to check that the group state is not changed
        this.columnGroupHeaderLayer.expandGroup(4);

        Group group = this.columnGroupHeaderLayer.getGroupByPosition(4);
        assertFalse(group.isCollapsed());
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(4, group.getVisibleSpan());

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
        assertEquals(4, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnPosition());
        assertEquals(4, cell.getColumnIndex());
        assertEquals(4, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
    }

    @Test
    public void shouldNotShowHiddenLastColumnInCollapsedGroup() {
        assertEquals(14, this.columnGroupExpandCollapseLayer.getColumnCount());

        // hide column in group
        if (this.selectionLayer.doCommand(new ColumnHideCommand(this.selectionLayer, 7))) {
            assertEquals(13, this.columnGroupExpandCollapseLayer.getColumnCount());

            // collapse group
            this.columnGroupHeaderLayer.collapseGroup(4);

            assertEquals(11, this.columnGroupExpandCollapseLayer.getColumnCount());

            Group group = this.columnGroupHeaderLayer.getGroupByPosition(4);
            assertTrue(group.isCollapsed());
            assertEquals(4, group.getStartIndex());
            assertEquals(4, group.getVisibleStartIndex());
            assertEquals(4, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(1, group.getVisibleSpan());

            ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
            assertEquals(4, cell.getOriginColumnPosition());
            assertEquals(4, cell.getColumnPosition());
            assertEquals(4, cell.getColumnIndex());
            assertEquals(1, cell.getColumnSpan());
            assertEquals(1, cell.getRowSpan());
        } else {
            fail("Column not hidden");
        }

        // show all columns again
        // collapsed columns should stay hidden
        if (this.gridLayer.doCommand(new ShowAllColumnsCommand())) {
            assertEquals(11, this.columnGroupExpandCollapseLayer.getColumnCount());

            Group group = this.columnGroupHeaderLayer.getGroupByPosition(4);
            assertTrue(group.isCollapsed());
            assertEquals(4, group.getStartIndex());
            assertEquals(4, group.getVisibleStartIndex());
            assertEquals(4, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(1, group.getVisibleSpan());

            ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
            assertEquals(4, cell.getOriginColumnPosition());
            assertEquals(4, cell.getColumnPosition());
            assertEquals(4, cell.getColumnIndex());
            assertEquals(1, cell.getColumnSpan());
            assertEquals(1, cell.getRowSpan());
        } else {
            fail("Columns not shown again");
        }

        // expand again to check that the group state is not changed
        this.columnGroupHeaderLayer.expandGroup(4);

        Group group = this.columnGroupHeaderLayer.getGroupByPosition(4);
        assertFalse(group.isCollapsed());
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(4, group.getVisibleSpan());

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
        assertEquals(4, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnPosition());
        assertEquals(4, cell.getColumnIndex());
        assertEquals(4, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
    }

    @Test
    public void shouldNotShowHiddenColumnsInMultipleGroups() {
        assertEquals(14, this.columnGroupExpandCollapseLayer.getColumnCount());

        // hide last column in first group and first column in second group
        if (this.selectionLayer.doCommand(new MultiColumnHideCommand(this.selectionLayer, 3, 4))) {
            assertEquals(12, this.columnGroupExpandCollapseLayer.getColumnCount());

            // collapse group
            this.columnGroupHeaderLayer.collapseGroup(4);
            this.columnGroupHeaderLayer.collapseGroup(0);

            assertEquals(8, this.columnGroupExpandCollapseLayer.getColumnCount());

            Group group1 = this.columnGroupHeaderLayer.getGroupByPosition(0);
            assertTrue(group1.isCollapsed());
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(4, group1.getOriginalSpan());
            assertEquals(1, group1.getVisibleSpan());

            ILayerCell cell1 = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell1.getOriginColumnPosition());
            assertEquals(0, cell1.getColumnPosition());
            assertEquals(0, cell1.getColumnIndex());
            assertEquals(1, cell1.getColumnSpan());
            assertEquals(1, cell1.getRowSpan());

            Group group2 = this.columnGroupHeaderLayer.getGroupByPosition(1);
            assertTrue(group2.isCollapsed());
            assertEquals(4, group2.getStartIndex());
            assertEquals(5, group2.getVisibleStartIndex());
            assertEquals(1, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(1, group2.getVisibleSpan());

            ILayerCell cell2 = this.columnGroupHeaderLayer.getCellByPosition(1, 0);
            assertEquals(1, cell2.getOriginColumnPosition());
            assertEquals(1, cell2.getColumnPosition());
            assertEquals(5, cell2.getColumnIndex());
            assertEquals(1, cell2.getColumnSpan());
            assertEquals(1, cell2.getRowSpan());
        } else {
            fail("Column not hidden");
        }

        // show all columns again
        // collapsed columns should stay hidden
        if (this.gridLayer.doCommand(new ShowAllColumnsCommand())) {
            assertEquals(8, this.columnGroupExpandCollapseLayer.getColumnCount());

            Group group1 = this.columnGroupHeaderLayer.getGroupByPosition(0);
            assertTrue(group1.isCollapsed());
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(4, group1.getOriginalSpan());
            assertEquals(1, group1.getVisibleSpan());

            ILayerCell cell1 = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell1.getOriginColumnPosition());
            assertEquals(0, cell1.getColumnPosition());
            assertEquals(0, cell1.getColumnIndex());
            assertEquals(1, cell1.getColumnSpan());
            assertEquals(1, cell1.getRowSpan());

            Group group2 = this.columnGroupHeaderLayer.getGroupByPosition(1);
            assertTrue(group2.isCollapsed());
            assertEquals(4, group2.getStartIndex());
            assertEquals(4, group2.getVisibleStartIndex());
            assertEquals(1, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(1, group2.getVisibleSpan());

            ILayerCell cell2 = this.columnGroupHeaderLayer.getCellByPosition(1, 0);
            assertEquals(1, cell2.getOriginColumnPosition());
            assertEquals(1, cell2.getColumnPosition());
            assertEquals(4, cell2.getColumnIndex());
            assertEquals(1, cell2.getColumnSpan());
            assertEquals(1, cell2.getRowSpan());
        } else {
            fail("Columns not shown again");
        }

        // expand again to check that the group state is not changed
        this.columnGroupHeaderLayer.expandGroup(1);
        this.columnGroupHeaderLayer.expandGroup(0);

        Group group1 = this.columnGroupHeaderLayer.getGroupByPosition(0);
        assertFalse(group1.isCollapsed());
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());

        ILayerCell cell1 = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell1.getOriginColumnPosition());
        assertEquals(0, cell1.getColumnPosition());
        assertEquals(0, cell1.getColumnIndex());
        assertEquals(4, cell1.getColumnSpan());
        assertEquals(1, cell1.getRowSpan());

        Group group2 = this.columnGroupHeaderLayer.getGroupByPosition(4);
        assertFalse(group2.isCollapsed());
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());

        ILayerCell cell2 = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
        assertEquals(4, cell2.getOriginColumnPosition());
        assertEquals(4, cell2.getColumnPosition());
        assertEquals(4, cell2.getColumnIndex());
        assertEquals(4, cell2.getColumnSpan());
        assertEquals(1, cell2.getRowSpan());
    }

    @Test
    public void shouldShowNonGroupColumnIfAdjacentGroupsAreCollapsed() {
        // remove a column between two groups
        this.columnGroupHeaderLayer.removePositionsFromGroup(0, 3);

        // hide that column
        if (this.selectionLayer.doCommand(new ColumnHideCommand(this.selectionLayer, 3))) {
            assertEquals(13, this.columnGroupExpandCollapseLayer.getColumnCount());

            Group group1 = this.columnGroupHeaderLayer.getGroupByPosition(0);
            assertFalse(group1.isCollapsed());
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(3, group1.getOriginalSpan());
            assertEquals(3, group1.getVisibleSpan());

            ILayerCell cell1 = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell1.getOriginColumnPosition());
            assertEquals(0, cell1.getColumnPosition());
            assertEquals(0, cell1.getColumnIndex());
            assertEquals(3, cell1.getColumnSpan());
            assertEquals(1, cell1.getRowSpan());

            Group group2 = this.columnGroupHeaderLayer.getGroupByPosition(3);
            assertFalse(group2.isCollapsed());
            assertEquals(4, group2.getStartIndex());
            assertEquals(4, group2.getVisibleStartIndex());
            assertEquals(3, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(4, group2.getVisibleSpan());

            ILayerCell cell2 = this.columnGroupHeaderLayer.getCellByPosition(3, 0);
            assertEquals(3, cell2.getOriginColumnPosition());
            assertEquals(3, cell2.getColumnPosition());
            assertEquals(4, cell2.getColumnIndex());
            assertEquals(4, cell2.getColumnSpan());
            assertEquals(1, cell2.getRowSpan());
        } else {
            fail("Column not hidden");
        }

        // collapse both groups
        this.columnGroupHeaderLayer.collapseGroup(4);
        this.columnGroupHeaderLayer.collapseGroup(0);

        assertEquals(8, this.columnGroupExpandCollapseLayer.getColumnCount());

        Group group11 = this.columnGroupHeaderLayer.getGroupByPosition(0);
        assertTrue(group11.isCollapsed());
        assertEquals(0, group11.getStartIndex());
        assertEquals(0, group11.getVisibleStartIndex());
        assertEquals(0, group11.getVisibleStartPosition());
        assertEquals(3, group11.getOriginalSpan());
        assertEquals(1, group11.getVisibleSpan());

        ILayerCell cell11 = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell11.getOriginColumnPosition());
        assertEquals(0, cell11.getColumnPosition());
        assertEquals(0, cell11.getColumnIndex());
        assertEquals(1, cell11.getColumnSpan());
        assertEquals(1, cell11.getRowSpan());

        Group group22 = this.columnGroupHeaderLayer.getGroupByPosition(1);
        assertTrue(group22.isCollapsed());
        assertEquals(4, group22.getStartIndex());
        assertEquals(4, group22.getVisibleStartIndex());
        assertEquals(1, group22.getVisibleStartPosition());
        assertEquals(4, group22.getOriginalSpan());
        assertEquals(1, group22.getVisibleSpan());

        ILayerCell cell22 = this.columnGroupHeaderLayer.getCellByPosition(1, 0);
        assertEquals(1, cell22.getOriginColumnPosition());
        assertEquals(1, cell22.getColumnPosition());
        assertEquals(4, cell22.getColumnIndex());
        assertEquals(1, cell22.getColumnSpan());
        assertEquals(1, cell22.getRowSpan());

        // show all columns again
        if (this.gridLayer.doCommand(new ShowAllColumnsCommand())) {
            assertEquals(9, this.columnGroupExpandCollapseLayer.getColumnCount());

            Group group1 = this.columnGroupHeaderLayer.getGroupByPosition(0);
            assertTrue(group1.isCollapsed());
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(3, group1.getOriginalSpan());
            assertEquals(1, group1.getVisibleSpan());

            ILayerCell cell1 = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell1.getOriginColumnPosition());
            assertEquals(0, cell1.getColumnPosition());
            assertEquals(0, cell1.getColumnIndex());
            assertEquals(1, cell1.getColumnSpan());
            assertEquals(1, cell1.getRowSpan());

            Group group2 = this.columnGroupHeaderLayer.getGroupByPosition(2);
            assertTrue(group2.isCollapsed());
            assertEquals(4, group2.getStartIndex());
            assertEquals(4, group2.getVisibleStartIndex());
            assertEquals(2, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(1, group2.getVisibleSpan());

            ILayerCell cell2 = this.columnGroupHeaderLayer.getCellByPosition(2, 0);
            assertEquals(2, cell2.getOriginColumnPosition());
            assertEquals(2, cell2.getColumnPosition());
            assertEquals(4, cell2.getColumnIndex());
            assertEquals(1, cell2.getColumnSpan());
            assertEquals(1, cell2.getRowSpan());

            ILayerCell cell3 = this.columnGroupHeaderLayer.getCellByPosition(1, 0);
            assertEquals(1, cell3.getOriginColumnPosition());
            assertEquals(1, cell3.getColumnPosition());
            assertEquals(3, cell3.getColumnIndex());
            assertEquals(1, cell3.getColumnSpan());
            assertEquals(2, cell3.getRowSpan());
            assertEquals("Married", cell3.getDataValue());
        } else {
            fail("Columns not shown again");
        }

        // expand both groups again
        this.columnGroupHeaderLayer.expandGroup(2);
        this.columnGroupHeaderLayer.expandGroup(0);

        assertEquals(14, this.columnGroupExpandCollapseLayer.getColumnCount());

        group11 = this.columnGroupHeaderLayer.getGroupByPosition(0);
        assertFalse(group11.isCollapsed());
        assertEquals(0, group11.getStartIndex());
        assertEquals(0, group11.getVisibleStartIndex());
        assertEquals(0, group11.getVisibleStartPosition());
        assertEquals(3, group11.getOriginalSpan());
        assertEquals(3, group11.getVisibleSpan());

        cell11 = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell11.getOriginColumnPosition());
        assertEquals(0, cell11.getColumnPosition());
        assertEquals(0, cell11.getColumnIndex());
        assertEquals(3, cell11.getColumnSpan());
        assertEquals(1, cell11.getRowSpan());

        group22 = this.columnGroupHeaderLayer.getGroupByPosition(4);
        assertFalse(group22.isCollapsed());
        assertEquals(4, group22.getStartIndex());
        assertEquals(4, group22.getVisibleStartIndex());
        assertEquals(4, group22.getVisibleStartPosition());
        assertEquals(4, group22.getOriginalSpan());
        assertEquals(4, group22.getVisibleSpan());

        cell22 = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
        assertEquals(4, cell22.getOriginColumnPosition());
        assertEquals(4, cell22.getColumnPosition());
        assertEquals(4, cell22.getColumnIndex());
        assertEquals(4, cell22.getColumnSpan());
        assertEquals(1, cell22.getRowSpan());

        ILayerCell cell33 = this.columnGroupHeaderLayer.getCellByPosition(3, 0);
        assertEquals(3, cell33.getOriginColumnPosition());
        assertEquals(3, cell33.getColumnPosition());
        assertEquals(3, cell33.getColumnIndex());
        assertEquals(1, cell33.getColumnSpan());
        assertEquals(2, cell33.getRowSpan());
        assertEquals("Married", cell33.getDataValue());
    }

    @Test
    public void shouldOnlyShowNonGroupColumnIfAdjacentGroupsAreCollapsed() {
        // remove a column between two groups
        this.columnGroupHeaderLayer.removePositionsFromGroup(0, 3);

        // hide the last column of the first group
        // hide the non grouped column
        // hide the first column of the second groupd
        if (this.selectionLayer.doCommand(new MultiColumnHideCommand(this.selectionLayer, 2, 3, 4))) {
            assertEquals(11, this.columnGroupExpandCollapseLayer.getColumnCount());

            Group group1 = this.columnGroupHeaderLayer.getGroupByPosition(0);
            assertFalse(group1.isCollapsed());
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(3, group1.getOriginalSpan());
            assertEquals(2, group1.getVisibleSpan());

            ILayerCell cell1 = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell1.getOriginColumnPosition());
            assertEquals(0, cell1.getColumnPosition());
            assertEquals(0, cell1.getColumnIndex());
            assertEquals(2, cell1.getColumnSpan());
            assertEquals(1, cell1.getRowSpan());

            Group group2 = this.columnGroupHeaderLayer.getGroupByPosition(2);
            assertFalse(group2.isCollapsed());
            assertEquals(4, group2.getStartIndex());
            assertEquals(5, group2.getVisibleStartIndex());
            assertEquals(2, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(3, group2.getVisibleSpan());

            ILayerCell cell2 = this.columnGroupHeaderLayer.getCellByPosition(2, 0);
            assertEquals(2, cell2.getOriginColumnPosition());
            assertEquals(2, cell2.getColumnPosition());
            assertEquals(5, cell2.getColumnIndex());
            assertEquals(3, cell2.getColumnSpan());
            assertEquals(1, cell2.getRowSpan());
        } else {
            fail("Column not hidden");
        }

        // collapse both groups
        this.columnGroupHeaderLayer.collapseGroup(2);
        this.columnGroupHeaderLayer.collapseGroup(0);

        assertEquals(8, this.columnGroupExpandCollapseLayer.getColumnCount());

        Group group11 = this.columnGroupHeaderLayer.getGroupByPosition(0);
        assertTrue(group11.isCollapsed());
        assertEquals(0, group11.getStartIndex());
        assertEquals(0, group11.getVisibleStartIndex());
        assertEquals(0, group11.getVisibleStartPosition());
        assertEquals(3, group11.getOriginalSpan());
        assertEquals(1, group11.getVisibleSpan());

        ILayerCell cell11 = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell11.getOriginColumnPosition());
        assertEquals(0, cell11.getColumnPosition());
        assertEquals(0, cell11.getColumnIndex());
        assertEquals(1, cell11.getColumnSpan());
        assertEquals(1, cell11.getRowSpan());

        Group group22 = this.columnGroupHeaderLayer.getGroupByPosition(1);
        assertTrue(group22.isCollapsed());
        assertEquals(4, group22.getStartIndex());
        assertEquals(5, group22.getVisibleStartIndex());
        assertEquals(1, group22.getVisibleStartPosition());
        assertEquals(4, group22.getOriginalSpan());
        assertEquals(1, group22.getVisibleSpan());

        ILayerCell cell22 = this.columnGroupHeaderLayer.getCellByPosition(1, 0);
        assertEquals(1, cell22.getOriginColumnPosition());
        assertEquals(1, cell22.getColumnPosition());
        assertEquals(5, cell22.getColumnIndex());
        assertEquals(1, cell22.getColumnSpan());
        assertEquals(1, cell22.getRowSpan());

        // show all columns again
        if (this.gridLayer.doCommand(new ShowAllColumnsCommand())) {
            assertEquals(9, this.columnGroupExpandCollapseLayer.getColumnCount());

            Group group1 = this.columnGroupHeaderLayer.getGroupByPosition(0);
            assertTrue(group1.isCollapsed());
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(3, group1.getOriginalSpan());
            assertEquals(1, group1.getVisibleSpan());

            ILayerCell cell1 = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell1.getOriginColumnPosition());
            assertEquals(0, cell1.getColumnPosition());
            assertEquals(0, cell1.getColumnIndex());
            assertEquals(1, cell1.getColumnSpan());
            assertEquals(1, cell1.getRowSpan());

            Group group2 = this.columnGroupHeaderLayer.getGroupByPosition(2);
            assertTrue(group2.isCollapsed());
            assertEquals(4, group2.getStartIndex());
            assertEquals(4, group2.getVisibleStartIndex());
            assertEquals(2, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(1, group2.getVisibleSpan());

            ILayerCell cell2 = this.columnGroupHeaderLayer.getCellByPosition(2, 0);
            assertEquals(2, cell2.getOriginColumnPosition());
            assertEquals(2, cell2.getColumnPosition());
            assertEquals(4, cell2.getColumnIndex());
            assertEquals(1, cell2.getColumnSpan());
            assertEquals(1, cell2.getRowSpan());

            ILayerCell cell3 = this.columnGroupHeaderLayer.getCellByPosition(1, 0);
            assertEquals(1, cell3.getOriginColumnPosition());
            assertEquals(1, cell3.getColumnPosition());
            assertEquals(3, cell3.getColumnIndex());
            assertEquals(1, cell3.getColumnSpan());
            assertEquals(2, cell3.getRowSpan());
            assertEquals("Married", cell3.getDataValue());
        } else {
            fail("Columns not shown again");
        }

        // expand both groups again
        this.columnGroupHeaderLayer.expandGroup(2);
        this.columnGroupHeaderLayer.expandGroup(0);

        assertEquals(14, this.columnGroupExpandCollapseLayer.getColumnCount());

        group11 = this.columnGroupHeaderLayer.getGroupByPosition(0);
        assertFalse(group11.isCollapsed());
        assertEquals(0, group11.getStartIndex());
        assertEquals(0, group11.getVisibleStartIndex());
        assertEquals(0, group11.getVisibleStartPosition());
        assertEquals(3, group11.getOriginalSpan());
        assertEquals(3, group11.getVisibleSpan());

        cell11 = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell11.getOriginColumnPosition());
        assertEquals(0, cell11.getColumnPosition());
        assertEquals(0, cell11.getColumnIndex());
        assertEquals(3, cell11.getColumnSpan());
        assertEquals(1, cell11.getRowSpan());

        group22 = this.columnGroupHeaderLayer.getGroupByPosition(4);
        assertFalse(group22.isCollapsed());
        assertEquals(4, group22.getStartIndex());
        assertEquals(4, group22.getVisibleStartIndex());
        assertEquals(4, group22.getVisibleStartPosition());
        assertEquals(4, group22.getOriginalSpan());
        assertEquals(4, group22.getVisibleSpan());

        cell22 = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
        assertEquals(4, cell22.getOriginColumnPosition());
        assertEquals(4, cell22.getColumnPosition());
        assertEquals(4, cell22.getColumnIndex());
        assertEquals(4, cell22.getColumnSpan());
        assertEquals(1, cell22.getRowSpan());

        ILayerCell cell33 = this.columnGroupHeaderLayer.getCellByPosition(3, 0);
        assertEquals(3, cell33.getOriginColumnPosition());
        assertEquals(3, cell33.getColumnPosition());
        assertEquals(3, cell33.getColumnIndex());
        assertEquals(1, cell33.getColumnSpan());
        assertEquals(2, cell33.getRowSpan());
        assertEquals("Married", cell33.getDataValue());
    }

    @Test
    public void shouldHideStaticColumnInCollapsedState() {
        // set last two columns in second group as static
        this.columnGroupHeaderLayer.addStaticColumnIndexesToGroup(0, 4, 6, 7);

        assertEquals(14, this.columnGroupExpandCollapseLayer.getColumnCount());

        // collapse group with static indexes
        this.columnGroupHeaderLayer.collapseGroup(4);

        assertEquals(12, this.columnGroupExpandCollapseLayer.getColumnCount());

        Group group = this.columnGroupHeaderLayer.getGroupByPosition(4);
        assertTrue(group.isCollapsed());
        assertEquals(4, group.getStartIndex());
        assertEquals(6, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(2, group.getVisibleSpan());

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
        assertEquals(4, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnPosition());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());

        // hide first static column
        if (this.selectionLayer.doCommand(new ColumnHideCommand(this.selectionLayer, 4))) {
            group = this.columnGroupHeaderLayer.getGroupByPosition(4);
            assertTrue(group.isCollapsed());
            assertEquals(4, group.getStartIndex());
            assertEquals(7, group.getVisibleStartIndex());
            assertEquals(4, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(1, group.getVisibleSpan());

            cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
            assertEquals(4, cell.getOriginColumnPosition());
            assertEquals(4, cell.getColumnPosition());
            assertEquals(7, cell.getColumnIndex());
            assertEquals(1, cell.getColumnSpan());
            assertEquals(1, cell.getRowSpan());
        } else {
            fail("Column not hidden");
        }

        // expand group with static indexes
        this.columnGroupHeaderLayer.expandGroup(4);

        assertEquals(13, this.columnGroupExpandCollapseLayer.getColumnCount());

        group = this.columnGroupHeaderLayer.getGroupByPosition(4);
        assertFalse(group.isCollapsed());
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());

        cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
        assertEquals(4, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnPosition());
        assertEquals(3, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
    }

    @Test
    public void shouldShowHiddenFirstStaticColumnInCollapsedState() {
        // set last two columns in second group as static
        this.columnGroupHeaderLayer.addStaticColumnIndexesToGroup(0, 4, 6, 7);

        assertEquals(14, this.columnGroupExpandCollapseLayer.getColumnCount());

        // collapse group with static indexes
        this.columnGroupHeaderLayer.collapseGroup(4);

        assertEquals(12, this.columnGroupExpandCollapseLayer.getColumnCount());

        Group group = this.columnGroupHeaderLayer.getGroupByPosition(4);
        assertTrue(group.isCollapsed());
        assertEquals(4, group.getStartIndex());
        assertEquals(6, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(2, group.getVisibleSpan());

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
        assertEquals(4, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnPosition());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());

        // hide first static column
        if (this.selectionLayer.doCommand(new ColumnHideCommand(this.selectionLayer, 4))) {
            group = this.columnGroupHeaderLayer.getGroupByPosition(4);
            assertTrue(group.isCollapsed());
            assertEquals(4, group.getStartIndex());
            assertEquals(7, group.getVisibleStartIndex());
            assertEquals(4, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(1, group.getVisibleSpan());

            cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
            assertEquals(4, cell.getOriginColumnPosition());
            assertEquals(4, cell.getColumnPosition());
            assertEquals(7, cell.getColumnIndex());
            assertEquals(1, cell.getColumnSpan());
            assertEquals(1, cell.getRowSpan());
        } else {
            fail("Column not hidden");
        }

        // show all columns again
        if (this.gridLayer.doCommand(new ShowAllColumnsCommand())) {
            group = this.columnGroupHeaderLayer.getGroupByPosition(4);
            assertTrue(group.isCollapsed());
            assertEquals(4, group.getStartIndex());
            assertEquals(6, group.getVisibleStartIndex());
            assertEquals(4, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(2, group.getVisibleSpan());

            cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
            assertEquals(4, cell.getOriginColumnPosition());
            assertEquals(4, cell.getColumnPosition());
            assertEquals(6, cell.getColumnIndex());
            assertEquals(2, cell.getColumnSpan());
            assertEquals(1, cell.getRowSpan());
        } else {
            fail("Columns not shown again");
        }

        // expand group with static indexes
        this.columnGroupHeaderLayer.expandGroup(4);

        assertEquals(14, this.columnGroupExpandCollapseLayer.getColumnCount());

        group = this.columnGroupHeaderLayer.getGroupByPosition(4);
        assertFalse(group.isCollapsed());
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(4, group.getVisibleSpan());

        cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
        assertEquals(4, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnPosition());
        assertEquals(4, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
    }

    @Test
    public void shouldShowHiddenLastStaticColumnInCollapsedState() {
        // set last two columns in second group as static
        this.columnGroupHeaderLayer.addStaticColumnIndexesToGroup(0, 4, 6, 7);

        assertEquals(14, this.columnGroupExpandCollapseLayer.getColumnCount());

        // collapse group with static indexes
        this.columnGroupHeaderLayer.collapseGroup(4);

        assertEquals(12, this.columnGroupExpandCollapseLayer.getColumnCount());

        Group group = this.columnGroupHeaderLayer.getGroupByPosition(4);
        assertTrue(group.isCollapsed());
        assertEquals(4, group.getStartIndex());
        assertEquals(6, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(2, group.getVisibleSpan());

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
        assertEquals(4, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnPosition());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());

        // hide last static column
        if (this.selectionLayer.doCommand(new ColumnHideCommand(this.selectionLayer, 5))) {
            group = this.columnGroupHeaderLayer.getGroupByPosition(4);
            assertTrue(group.isCollapsed());
            assertEquals(4, group.getStartIndex());
            assertEquals(6, group.getVisibleStartIndex());
            assertEquals(4, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(1, group.getVisibleSpan());

            cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
            assertEquals(4, cell.getOriginColumnPosition());
            assertEquals(4, cell.getColumnPosition());
            assertEquals(6, cell.getColumnIndex());
            assertEquals(1, cell.getColumnSpan());
            assertEquals(1, cell.getRowSpan());
        } else {
            fail("Column not hidden");
        }

        // show all columns again
        if (this.gridLayer.doCommand(new ShowAllColumnsCommand())) {
            group = this.columnGroupHeaderLayer.getGroupByPosition(4);
            assertTrue(group.isCollapsed());
            assertEquals(4, group.getStartIndex());
            assertEquals(6, group.getVisibleStartIndex());
            assertEquals(4, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(2, group.getVisibleSpan());

            cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
            assertEquals(4, cell.getOriginColumnPosition());
            assertEquals(4, cell.getColumnPosition());
            assertEquals(6, cell.getColumnIndex());
            assertEquals(2, cell.getColumnSpan());
            assertEquals(1, cell.getRowSpan());
        } else {
            fail("Columns not shown again");
        }

        // expand group with static indexes
        this.columnGroupHeaderLayer.expandGroup(4);

        verifyCleanState();
    }

    @Test
    public void shouldShowAllHiddenStaticColumnsInCollapsedState() {
        // set last two columns in second group as static
        this.columnGroupHeaderLayer.addStaticColumnIndexesToGroup(0, 4, 6, 7);

        assertEquals(14, this.columnGroupExpandCollapseLayer.getColumnCount());

        // collapse group with static indexes
        this.columnGroupHeaderLayer.collapseGroup(4);

        assertEquals(12, this.columnGroupExpandCollapseLayer.getColumnCount());

        Group group = this.columnGroupHeaderLayer.getGroupByPosition(4);
        assertTrue(group.isCollapsed());
        assertEquals(4, group.getStartIndex());
        assertEquals(6, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(2, group.getVisibleSpan());

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
        assertEquals(4, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnPosition());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());

        // hide all static columns
        if (this.selectionLayer.doCommand(new MultiColumnHideCommand(this.selectionLayer, 4, 5))) {
            group = this.columnGroupHeaderLayer.getGroupByPosition(4);
            assertFalse(group.isCollapsed());
            assertEquals("Facts", group.getName());
            assertEquals(8, group.getStartIndex());
            assertEquals(8, group.getVisibleStartIndex());
            assertEquals(4, group.getVisibleStartPosition());
            assertEquals(3, group.getOriginalSpan());
            assertEquals(3, group.getVisibleSpan());

            cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
            assertEquals(4, cell.getOriginColumnPosition());
            assertEquals(4, cell.getColumnPosition());
            assertEquals(8, cell.getColumnIndex());
            assertEquals(3, cell.getColumnSpan());
            assertEquals(1, cell.getRowSpan());
        } else {
            fail("Column not hidden");
        }

        // show all columns again
        if (this.gridLayer.doCommand(new ShowAllColumnsCommand())) {
            group = this.columnGroupHeaderLayer.getGroupByPosition(4);
            assertTrue(group.isCollapsed());
            assertEquals(4, group.getStartIndex());
            assertEquals(6, group.getVisibleStartIndex());
            assertEquals(4, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(2, group.getVisibleSpan());

            cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
            assertEquals(4, cell.getOriginColumnPosition());
            assertEquals(4, cell.getColumnPosition());
            assertEquals(6, cell.getColumnIndex());
            assertEquals(2, cell.getColumnSpan());
            assertEquals(1, cell.getRowSpan());
        } else {
            fail("Columns not shown again");
        }

        // expand group with static indexes
        this.columnGroupHeaderLayer.expandGroup(4);

        assertEquals(14, this.columnGroupExpandCollapseLayer.getColumnCount());

        group = this.columnGroupHeaderLayer.getGroupByPosition(4);
        assertFalse(group.isCollapsed());
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(4, group.getVisibleSpan());

        cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
        assertEquals(4, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnPosition());
        assertEquals(4, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
    }

    @Test
    public void shouldHideShowFirstGroupInCollapsedState() {
        // collapse group without static indexes
        this.columnGroupHeaderLayer.collapseGroup(0);

        assertEquals(11, this.columnGroupExpandCollapseLayer.getColumnCount());

        Group group = this.columnGroupHeaderLayer.getGroupByPosition(0);
        assertTrue(group.isCollapsed());
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());

        // hide visible column in group
        if (this.selectionLayer.doCommand(new ColumnHideCommand(this.selectionLayer, 0))) {
            group = this.columnGroupHeaderLayer.getGroupByPosition(0);
            assertFalse(group.isCollapsed());
            assertEquals("Address", group.getName());
            assertEquals(4, group.getStartIndex());
            assertEquals(4, group.getVisibleStartIndex());
            assertEquals(0, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(4, group.getVisibleSpan());

            cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginColumnPosition());
            assertEquals(0, cell.getColumnPosition());
            assertEquals(4, cell.getColumnIndex());
            assertEquals(4, cell.getColumnSpan());
            assertEquals(1, cell.getRowSpan());

            // check group by name
            group = this.columnGroupHeaderLayer.getGroupByName("Person");
            assertNotNull(group);
            assertEquals(0, group.getStartIndex());
            assertEquals(-1, group.getVisibleStartIndex());
            assertEquals(-1, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(0, group.getVisibleSpan());
        } else {
            fail("Column not hidden");
        }

        // show all columns again
        if (this.gridLayer.doCommand(new ShowAllColumnsCommand())) {
            group = this.columnGroupHeaderLayer.getGroupByPosition(0);
            assertTrue(group.isCollapsed());
            assertEquals(0, group.getStartIndex());
            assertEquals(0, group.getVisibleStartIndex());
            assertEquals(0, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(1, group.getVisibleSpan());

            cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginColumnPosition());
            assertEquals(0, cell.getColumnPosition());
            assertEquals(0, cell.getColumnIndex());
            assertEquals(1, cell.getColumnSpan());
            assertEquals(1, cell.getRowSpan());
        } else {
            fail("Columns not shown again");
        }

        // expand group
        this.columnGroupHeaderLayer.expandGroup(0);

        assertEquals(14, this.columnGroupExpandCollapseLayer.getColumnCount());

        verifyCleanState();
    }

    @Test
    public void shouldHideShowLastGroupInCollapsedState() {
        // collapse last group without static indexes
        this.columnGroupHeaderLayer.collapseGroup(11);

        assertEquals(12, this.columnGroupExpandCollapseLayer.getColumnCount());

        Group group = this.columnGroupHeaderLayer.getGroupByPosition(11);
        assertTrue(group.isCollapsed());
        assertEquals(11, group.getStartIndex());
        assertEquals(11, group.getVisibleStartIndex());
        assertEquals(11, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(11, 0);
        assertEquals(11, cell.getOriginColumnPosition());
        assertEquals(11, cell.getColumnPosition());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());

        // hide visible column in group
        if (this.selectionLayer.doCommand(new ColumnHideCommand(this.selectionLayer, 11))) {
            assertEquals(11, this.columnGroupExpandCollapseLayer.getColumnCount());

            group = this.columnGroupHeaderLayer.getGroupByPosition(11);
            assertNull(group);

            // check group by name
            group = this.columnGroupHeaderLayer.getGroupByName("Personal");
            assertNotNull(group);
            // it is the last column so we where not able to update
            assertEquals(11, group.getStartIndex());
            assertEquals(-1, group.getVisibleStartIndex());
            assertEquals(-1, group.getVisibleStartPosition());
            assertEquals(3, group.getOriginalSpan());
            assertEquals(0, group.getVisibleSpan());
        } else {
            fail("Column not hidden");
        }

        // show all columns again
        if (this.gridLayer.doCommand(new ShowAllColumnsCommand())) {
            assertEquals(12, this.columnGroupExpandCollapseLayer.getColumnCount());

            group = this.columnGroupHeaderLayer.getGroupByPosition(11);
            assertTrue(group.isCollapsed());
            assertEquals(11, group.getStartIndex());
            assertEquals(11, group.getVisibleStartIndex());
            assertEquals(11, group.getVisibleStartPosition());
            assertEquals(3, group.getOriginalSpan());
            assertEquals(1, group.getVisibleSpan());

            cell = this.columnGroupHeaderLayer.getCellByPosition(11, 0);
            assertEquals(11, cell.getOriginColumnPosition());
            assertEquals(11, cell.getColumnPosition());
            assertEquals(11, cell.getColumnIndex());
            assertEquals(1, cell.getColumnSpan());
            assertEquals(1, cell.getRowSpan());
        } else {
            fail("Columns not shown again");
        }

        // scroll to show the last column only a bit

        // expand group
        this.columnGroupHeaderLayer.expandGroup(11);

        assertEquals(14, this.columnGroupExpandCollapseLayer.getColumnCount());

        verifyCleanState();
    }

    @Test
    public void shouldHideShowLastGroupInCollapsedStateWithStatics() {
        this.columnGroupHeaderLayer.addStaticColumnIndexesToGroup(0, 11, 12, 13);

        // collapse last group with static indexes
        this.columnGroupHeaderLayer.collapseGroup(11);

        assertEquals(13, this.columnGroupExpandCollapseLayer.getColumnCount());

        Group group = this.columnGroupHeaderLayer.getGroupByPosition(11);
        assertTrue(group.isCollapsed());
        assertEquals(11, group.getStartIndex());
        assertEquals(12, group.getVisibleStartIndex());
        assertEquals(11, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(2, group.getVisibleSpan());

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(11, 0);
        assertEquals(11, cell.getOriginColumnPosition());
        assertEquals(11, cell.getColumnPosition());
        assertEquals(12, cell.getColumnIndex());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());

        // hide visible column in group
        if (this.selectionLayer.doCommand(new MultiColumnHideCommand(this.selectionLayer, 11, 12))) {
            assertEquals(11, this.columnGroupExpandCollapseLayer.getColumnCount());

            group = this.columnGroupHeaderLayer.getGroupByPosition(11);
            assertNull(group);

            // check group by name
            group = this.columnGroupHeaderLayer.getGroupByName("Personal");
            assertNotNull(group);
            // it is the last column so we where not able to update
            assertEquals(11, group.getStartIndex());
            assertEquals(-1, group.getVisibleStartIndex());
            assertEquals(-1, group.getVisibleStartPosition());
            assertEquals(3, group.getOriginalSpan());
            assertEquals(0, group.getVisibleSpan());
        } else {
            fail("Column not hidden");
        }

        // show all columns again
        if (this.gridLayer.doCommand(new ShowAllColumnsCommand())) {
            assertEquals(13, this.columnGroupExpandCollapseLayer.getColumnCount());

            group = this.columnGroupHeaderLayer.getGroupByPosition(11);
            assertTrue(group.isCollapsed());
            assertEquals(11, group.getStartIndex());
            assertEquals(12, group.getVisibleStartIndex());
            assertEquals(11, group.getVisibleStartPosition());
            assertEquals(3, group.getOriginalSpan());
            assertEquals(2, group.getVisibleSpan());

            cell = this.columnGroupHeaderLayer.getCellByPosition(11, 0);
            assertEquals(11, cell.getOriginColumnPosition());
            assertEquals(11, cell.getColumnPosition());
            assertEquals(12, cell.getColumnIndex());
            assertEquals(2, cell.getColumnSpan());
            assertEquals(1, cell.getRowSpan());
        } else {
            fail("Columns not shown again");
        }

        // scroll to show the last column only a bit

        // expand group
        this.columnGroupHeaderLayer.expandGroup(11);

        assertEquals(14, this.columnGroupExpandCollapseLayer.getColumnCount());

        verifyCleanState();
    }

    @Test
    public void shouldHideLastColumnInLastGroup() {
        // special test case for hide operations at the end of a table. The
        // HideColumnEvent is not transported up to the ColumnGroupHeaderLayer
        // because the conversion is not able to convert the position outside
        // the new structure and the start position will be -1

        // increase visible area to show all
        this.gridLayer.setClientAreaProvider(new IClientAreaProvider() {

            @Override
            public Rectangle getClientArea() {
                return new Rectangle(0, 0, 1500, 250);
            }

        });

        if (this.selectionLayer.doCommand(new ColumnHideCommand(this.selectionLayer, 13))) {
            ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(11, 0);
            assertEquals(11, cell.getOriginColumnPosition());
            assertEquals("Personal", cell.getDataValue());
            assertEquals(1100, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(2, cell.getColumnSpan());
            assertEquals(200, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);
        } else {
            fail("Column not hidden");
        }

        // show again
        if (this.gridLayer.doCommand(new ShowAllColumnsCommand())) {
            // modifed verifyCleanState as we changed the client area
            ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginColumnPosition());
            assertEquals(4, cell.getColumnSpan());
            assertEquals("Person", cell.getDataValue());
            assertEquals(0, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(400, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
            assertEquals(4, cell.getOriginColumnPosition());
            assertEquals(4, cell.getColumnSpan());
            assertEquals("Address", cell.getDataValue());
            assertEquals(400, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(400, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            cell = this.columnGroupHeaderLayer.getCellByPosition(8, 0);
            assertEquals(8, cell.getOriginColumnPosition());
            assertEquals(3, cell.getColumnSpan());
            assertEquals("Facts", cell.getDataValue());
            assertEquals(800, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(300, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            cell = this.columnGroupHeaderLayer.getCellByPosition(11, 0);
            assertEquals(11, cell.getOriginColumnPosition());
            assertEquals(3, cell.getColumnSpan());
            assertEquals("Personal", cell.getDataValue());
            assertEquals(1100, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(300, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(4, group1.getOriginalSpan());
            assertEquals(4, group1.getVisibleSpan());

            Group group2 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
            assertEquals(4, group2.getStartIndex());
            assertEquals(4, group2.getVisibleStartIndex());
            assertEquals(4, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(4, group2.getVisibleSpan());

            Group group3 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
            assertEquals(8, group3.getStartIndex());
            assertEquals(8, group3.getVisibleStartIndex());
            assertEquals(8, group3.getVisibleStartPosition());
            assertEquals(3, group3.getOriginalSpan());
            assertEquals(3, group3.getVisibleSpan());

            Group group4 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
            assertEquals(11, group4.getStartIndex());
            assertEquals(11, group4.getVisibleStartIndex());
            assertEquals(11, group4.getVisibleStartPosition());
            assertEquals(3, group4.getOriginalSpan());
            assertEquals(3, group4.getVisibleSpan());
        } else {
            fail("Columns not shown again");
        }
    }

    @Test
    public void shouldHideMultipleColumnsAfterFirstHideAtEndOfTable() {
        // special test case for hide operations at the end of a table. The
        // HideColumnEvent is not transported up to the ColumnGroupHeaderLayer
        // because the conversion is not able to convert the position outside
        // the new structure and the start position will be -1

        // increase visible area to show all
        this.gridLayer.setClientAreaProvider(new IClientAreaProvider() {

            @Override
            public Rectangle getClientArea() {
                return new Rectangle(0, 0, 1500, 250);
            }

        });

        // first hide the first column in the last group
        if (this.selectionLayer.doCommand(new ColumnHideCommand(this.selectionLayer, 11))) {
            ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(11, 0);
            assertEquals(11, cell.getOriginColumnPosition());
            assertEquals("Personal", cell.getDataValue());
            assertEquals(1100, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(2, cell.getColumnSpan());
            assertEquals(200, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);
        } else {
            fail("Column not hidden");
        }

        // now hide the last column of the previous group and the now first
        // column of the last group. this looks like a contiguous selection, but
        // internally there is a gap. the second range
        if (this.selectionLayer.doCommand(new MultiColumnHideCommand(this.selectionLayer, 10, 11))) {
            Group group1 = this.columnGroupHeaderLayer.getGroupByPosition(8);
            assertEquals(8, group1.getStartIndex());
            assertEquals(8, group1.getVisibleStartIndex());
            assertEquals(8, group1.getVisibleStartPosition());
            assertEquals(3, group1.getOriginalSpan());
            assertEquals(2, group1.getVisibleSpan());

            Group group2 = this.columnGroupHeaderLayer.getGroupByPosition(10);
            assertEquals(11, group2.getStartIndex());
            assertEquals(13, group2.getVisibleStartIndex());
            assertEquals(10, group2.getVisibleStartPosition());
            assertEquals(3, group2.getOriginalSpan());
            assertEquals(1, group2.getVisibleSpan());

            ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(8, 0);
            assertEquals(8, cell.getOriginColumnPosition());
            assertEquals(2, cell.getColumnSpan());
            assertEquals("Facts", cell.getDataValue());
            assertEquals(800, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(200, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            cell = this.columnGroupHeaderLayer.getCellByPosition(10, 0);
            assertEquals(10, cell.getOriginColumnPosition());
            assertEquals("Personal", cell.getDataValue());
            assertEquals(1000, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(1, cell.getColumnSpan());
            assertEquals(100, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);
        } else {
            fail("Column not hidden");
        }

        // show again
        if (this.gridLayer.doCommand(new ShowAllColumnsCommand())) {
            // modifed verifyCleanState as we changed the client area
            ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginColumnPosition());
            assertEquals(4, cell.getColumnSpan());
            assertEquals("Person", cell.getDataValue());
            assertEquals(0, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(400, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
            assertEquals(4, cell.getOriginColumnPosition());
            assertEquals(4, cell.getColumnSpan());
            assertEquals("Address", cell.getDataValue());
            assertEquals(400, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(400, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            cell = this.columnGroupHeaderLayer.getCellByPosition(8, 0);
            assertEquals(8, cell.getOriginColumnPosition());
            assertEquals(3, cell.getColumnSpan());
            assertEquals("Facts", cell.getDataValue());
            assertEquals(800, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(300, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            cell = this.columnGroupHeaderLayer.getCellByPosition(11, 0);
            assertEquals(11, cell.getOriginColumnPosition());
            assertEquals(3, cell.getColumnSpan());
            assertEquals("Personal", cell.getDataValue());
            assertEquals(1100, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(300, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(4, group1.getOriginalSpan());
            assertEquals(4, group1.getVisibleSpan());

            Group group2 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
            assertEquals(4, group2.getStartIndex());
            assertEquals(4, group2.getVisibleStartIndex());
            assertEquals(4, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(4, group2.getVisibleSpan());

            Group group3 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
            assertEquals(8, group3.getStartIndex());
            assertEquals(8, group3.getVisibleStartIndex());
            assertEquals(8, group3.getVisibleStartPosition());
            assertEquals(3, group3.getOriginalSpan());
            assertEquals(3, group3.getVisibleSpan());

            Group group4 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
            assertEquals(11, group4.getStartIndex());
            assertEquals(11, group4.getVisibleStartIndex());
            assertEquals(11, group4.getVisibleStartPosition());
            assertEquals(3, group4.getOriginalSpan());
            assertEquals(3, group4.getVisibleSpan());
        } else {
            fail("Columns not shown again");
        }
    }

    @Test
    public void shouldHideMultipleColumnsAfterCollapseWithStaticsAtEndOfTable() {
        // special test case for hide operations at the end of a table. The
        // HideColumnEvent is not transported up to the ColumnGroupHeaderLayer
        // because the conversion is not able to convert the position outside
        // the new structure and the start position will be -1

        // increase visible area to show all
        this.gridLayer.setClientAreaProvider(new IClientAreaProvider() {

            @Override
            public Rectangle getClientArea() {
                return new Rectangle(0, 0, 1500, 250);
            }

        });

        // set last two columns in the last group static
        this.columnGroupHeaderLayer.addStaticColumnIndexesToGroup(0, 11, 12, 13);

        // first collapse the last group
        this.columnGroupHeaderLayer.collapseGroup(11);

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(11, 0);
        assertEquals(11, cell.getOriginColumnPosition());
        assertEquals(12, cell.getColumnIndex());
        assertEquals("Personal", cell.getDataValue());
        assertEquals(1100, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(2, cell.getColumnSpan());
        assertEquals(200, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        Group group2 = this.columnGroupHeaderLayer.getGroupByPosition(11);
        assertTrue(group2.isCollapsed());
        assertEquals(11, group2.getStartIndex());
        assertEquals(12, group2.getVisibleStartIndex());
        assertEquals(11, group2.getVisibleStartPosition());
        assertEquals(3, group2.getOriginalSpan());
        assertEquals(2, group2.getVisibleSpan());

        // now hide the last column of the previous group and the now first
        // column of the last group. this looks like a contiguous selection, but
        // internally there is a gap. the second range
        if (this.selectionLayer.doCommand(new MultiColumnHideCommand(this.selectionLayer, 10, 11))) {
            Group group1 = this.columnGroupHeaderLayer.getGroupByPosition(8);
            assertEquals(8, group1.getStartIndex());
            assertEquals(8, group1.getVisibleStartIndex());
            assertEquals(8, group1.getVisibleStartPosition());
            assertEquals(3, group1.getOriginalSpan());
            assertEquals(2, group1.getVisibleSpan());

            group2 = this.columnGroupHeaderLayer.getGroupByPosition(10);
            assertEquals(11, group2.getStartIndex());
            assertEquals(13, group2.getVisibleStartIndex());
            assertEquals(10, group2.getVisibleStartPosition());
            assertEquals(3, group2.getOriginalSpan());
            assertEquals(1, group2.getVisibleSpan());

            cell = this.columnGroupHeaderLayer.getCellByPosition(8, 0);
            assertEquals(8, cell.getOriginColumnPosition());
            assertEquals(2, cell.getColumnSpan());
            assertEquals("Facts", cell.getDataValue());
            assertEquals(800, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(200, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            cell = this.columnGroupHeaderLayer.getCellByPosition(10, 0);
            assertEquals(10, cell.getOriginColumnPosition());
            assertEquals("Personal", cell.getDataValue());
            assertEquals(1000, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(1, cell.getColumnSpan());
            assertEquals(100, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);
        } else {
            fail("Column not hidden");
        }

        // show again
        if (this.gridLayer.doCommand(new ShowAllColumnsCommand())) {
            // modifed verifyCleanState as we changed the client area
            cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginColumnPosition());
            assertEquals(4, cell.getColumnSpan());
            assertEquals("Person", cell.getDataValue());
            assertEquals(0, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(400, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
            assertEquals(4, cell.getOriginColumnPosition());
            assertEquals(4, cell.getColumnSpan());
            assertEquals("Address", cell.getDataValue());
            assertEquals(400, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(400, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            cell = this.columnGroupHeaderLayer.getCellByPosition(8, 0);
            assertEquals(8, cell.getOriginColumnPosition());
            assertEquals(3, cell.getColumnSpan());
            assertEquals("Facts", cell.getDataValue());
            assertEquals(800, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(300, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            cell = this.columnGroupHeaderLayer.getCellByPosition(11, 0);
            assertEquals(11, cell.getOriginColumnPosition());
            assertEquals(2, cell.getColumnSpan());
            assertEquals("Personal", cell.getDataValue());
            assertEquals(1100, cell.getBounds().x);
            assertEquals(0, cell.getBounds().y);
            assertEquals(200, cell.getBounds().width);
            assertEquals(20, cell.getBounds().height);

            Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(4, group1.getOriginalSpan());
            assertEquals(4, group1.getVisibleSpan());

            group2 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
            assertEquals(4, group2.getStartIndex());
            assertEquals(4, group2.getVisibleStartIndex());
            assertEquals(4, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(4, group2.getVisibleSpan());

            Group group3 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
            assertEquals(8, group3.getStartIndex());
            assertEquals(8, group3.getVisibleStartIndex());
            assertEquals(8, group3.getVisibleStartPosition());
            assertEquals(3, group3.getOriginalSpan());
            assertEquals(3, group3.getVisibleSpan());

            Group group4 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
            assertTrue(group4.isCollapsed());
            assertEquals(11, group4.getStartIndex());
            assertEquals(12, group4.getVisibleStartIndex());
            assertEquals(11, group4.getVisibleStartPosition());
            assertEquals(3, group4.getOriginalSpan());
            assertEquals(2, group4.getVisibleSpan());
        } else {
            fail("Columns not shown again");
        }
    }

    @Test
    public void shouldExpandOnRemoveGroupByPosition() {
        this.columnGroupHeaderLayer.collapseGroup(4);

        Collection<Integer> hiddenColumnIndexes = this.columnGroupExpandCollapseLayer.getHiddenColumnIndexes();
        assertEquals(3, hiddenColumnIndexes.size());
        assertTrue(hiddenColumnIndexes.contains(Integer.valueOf(5)));
        assertTrue(hiddenColumnIndexes.contains(Integer.valueOf(6)));
        assertTrue(hiddenColumnIndexes.contains(Integer.valueOf(7)));

        assertEquals(11, this.selectionLayer.getColumnCount());

        this.columnGroupHeaderLayer.removeGroup(4);

        hiddenColumnIndexes = this.columnGroupExpandCollapseLayer.getHiddenColumnIndexes();
        assertTrue(hiddenColumnIndexes.isEmpty());

        assertEquals(14, this.selectionLayer.getColumnCount());
    }

    @Test
    public void shouldExpandOnRemoveGroupByName() {
        this.columnGroupHeaderLayer.collapseGroup(0);

        Collection<Integer> hiddenColumnIndexes = this.columnGroupExpandCollapseLayer.getHiddenColumnIndexes();
        assertEquals(3, hiddenColumnIndexes.size());
        assertTrue(hiddenColumnIndexes.contains(Integer.valueOf(1)));
        assertTrue(hiddenColumnIndexes.contains(Integer.valueOf(2)));
        assertTrue(hiddenColumnIndexes.contains(Integer.valueOf(3)));

        assertEquals(11, this.selectionLayer.getColumnCount());

        this.columnGroupHeaderLayer.removeGroup("Person");

        hiddenColumnIndexes = this.columnGroupExpandCollapseLayer.getHiddenColumnIndexes();
        assertTrue(hiddenColumnIndexes.isEmpty());

        assertEquals(14, this.selectionLayer.getColumnCount());
    }

    @Test
    public void shouldExpandOnRemovePositionFromGroup() {
        this.columnGroupHeaderLayer.collapseGroup("Address");

        Collection<Integer> hiddenColumnIndexes = this.columnGroupExpandCollapseLayer.getHiddenColumnIndexes();
        assertEquals(3, hiddenColumnIndexes.size());
        assertTrue(hiddenColumnIndexes.contains(Integer.valueOf(5)));
        assertTrue(hiddenColumnIndexes.contains(Integer.valueOf(6)));
        assertTrue(hiddenColumnIndexes.contains(Integer.valueOf(7)));

        assertEquals(11, this.selectionLayer.getColumnCount());

        // Note: we can only remove the visible position
        this.columnGroupHeaderLayer.removePositionsFromGroup(0, 4);

        hiddenColumnIndexes = this.columnGroupExpandCollapseLayer.getHiddenColumnIndexes();
        assertTrue(hiddenColumnIndexes.isEmpty());

        assertNull(this.columnGroupHeaderLayer.getGroupByPosition(4));

        Group group = this.columnGroupHeaderLayer.getGroupByPosition(5);
        assertEquals("Address", group.getName());
        assertEquals(5, group.getStartIndex());
        assertEquals(5, group.getVisibleStartIndex());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());

        assertEquals(14, this.selectionLayer.getColumnCount());
    }

    @Test
    public void shouldExpandOnRemovePositionsFromMultipleGroups() {
        this.columnGroupHeaderLayer.collapseGroup("Person");
        this.columnGroupHeaderLayer.collapseGroup("Address");

        Collection<Integer> hiddenColumnIndexes = this.columnGroupExpandCollapseLayer.getHiddenColumnIndexes();
        assertEquals(6, hiddenColumnIndexes.size());
        assertTrue(hiddenColumnIndexes.contains(Integer.valueOf(1)));
        assertTrue(hiddenColumnIndexes.contains(Integer.valueOf(2)));
        assertTrue(hiddenColumnIndexes.contains(Integer.valueOf(3)));
        assertTrue(hiddenColumnIndexes.contains(Integer.valueOf(5)));
        assertTrue(hiddenColumnIndexes.contains(Integer.valueOf(6)));
        assertTrue(hiddenColumnIndexes.contains(Integer.valueOf(7)));

        assertEquals(8, this.selectionLayer.getColumnCount());

        // Note: we can only remove the visible position
        this.columnGroupHeaderLayer.removePositionsFromGroup(0, 0, 1);

        hiddenColumnIndexes = this.columnGroupExpandCollapseLayer.getHiddenColumnIndexes();
        assertTrue(hiddenColumnIndexes.isEmpty());

        assertNull(this.columnGroupHeaderLayer.getGroupByPosition(0));
        assertNull(this.columnGroupHeaderLayer.getGroupByPosition(4));

        Group group = this.columnGroupHeaderLayer.getGroupByPosition(1);
        assertEquals("Person", group.getName());
        assertEquals(1, group.getStartIndex());
        assertEquals(1, group.getVisibleStartIndex());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());

        group = this.columnGroupHeaderLayer.getGroupByPosition(5);
        assertEquals("Address", group.getName());
        assertEquals(5, group.getStartIndex());
        assertEquals(5, group.getVisibleStartIndex());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());

        assertEquals(14, this.selectionLayer.getColumnCount());
    }

    @Test
    public void shouldExpandOnAddPositionToGroup() {
        this.columnGroupHeaderLayer.removePositionsFromGroup(0, 7);

        this.columnGroupHeaderLayer.collapseGroup("Address");

        Collection<Integer> hiddenColumnIndexes = this.columnGroupExpandCollapseLayer.getHiddenColumnIndexes();
        assertEquals(2, hiddenColumnIndexes.size());
        assertTrue(hiddenColumnIndexes.contains(Integer.valueOf(5)));
        assertTrue(hiddenColumnIndexes.contains(Integer.valueOf(6)));

        assertEquals(12, this.selectionLayer.getColumnCount());

        this.columnGroupHeaderLayer.addPositionsToGroup("Address", 7);

        hiddenColumnIndexes = this.columnGroupExpandCollapseLayer.getHiddenColumnIndexes();
        assertTrue(hiddenColumnIndexes.isEmpty());

        assertNotNull(this.columnGroupHeaderLayer.getGroupByPosition(4));

        verifyCleanState();
    }

    @Test
    public void shouldExpandOnClearGroups() {

        // increase visible area to show all
        this.gridLayer.setClientAreaProvider(new IClientAreaProvider() {

            @Override
            public Rectangle getClientArea() {
                return new Rectangle(0, 0, 1500, 250);
            }

        });

        this.columnGroupHeaderLayer.collapseGroup(11);
        this.columnGroupHeaderLayer.collapseGroup(8);
        this.columnGroupHeaderLayer.collapseGroup("Address");
        this.columnGroupHeaderLayer.collapseGroup("Person");

        Collection<Integer> hiddenColumnIndexes = this.columnGroupExpandCollapseLayer.getHiddenColumnIndexes();
        assertEquals(10, hiddenColumnIndexes.size());
        assertTrue(hiddenColumnIndexes.contains(Integer.valueOf(1)));
        assertTrue(hiddenColumnIndexes.contains(Integer.valueOf(2)));
        assertTrue(hiddenColumnIndexes.contains(Integer.valueOf(3)));
        assertTrue(hiddenColumnIndexes.contains(Integer.valueOf(5)));
        assertTrue(hiddenColumnIndexes.contains(Integer.valueOf(6)));
        assertTrue(hiddenColumnIndexes.contains(Integer.valueOf(7)));
        assertTrue(hiddenColumnIndexes.contains(Integer.valueOf(9)));
        assertTrue(hiddenColumnIndexes.contains(Integer.valueOf(10)));
        assertTrue(hiddenColumnIndexes.contains(Integer.valueOf(12)));
        assertTrue(hiddenColumnIndexes.contains(Integer.valueOf(13)));

        assertEquals(4, this.selectionLayer.getColumnCount());

        this.columnGroupHeaderLayer.clearAllGroups();

        hiddenColumnIndexes = this.columnGroupExpandCollapseLayer.getHiddenColumnIndexes();
        assertTrue(hiddenColumnIndexes.isEmpty());

        assertEquals(14, this.columnGroupHeaderLayer.getColumnCount());

        assertTrue(this.columnGroupHeaderLayer.getGroupModel().getGroups().isEmpty());

        ILayerCell cell = null;
        for (int i = 0; i < 14; i++) {
            cell = this.columnGroupHeaderLayer.getCellByPosition(i, 0);
            assertEquals(i, cell.getColumnPosition());
            assertEquals(1, cell.getColumnSpan());
            assertEquals(2, cell.getRowSpan());
        }
    }

    @Test
    public void shouldCollapseExpandAll() {
        this.columnGroupHeaderLayer.collapseAllGroups();

        assertEquals(4, this.columnGroupHeaderLayer.getColumnCount());

        // verify collapsed states
        Group group = this.columnGroupHeaderLayer.getGroupByPosition(0);
        assertNotNull(group);
        assertEquals("Person", group.getName());
        assertTrue(group.isCollapsed());
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());

        group = this.columnGroupHeaderLayer.getGroupByPosition(1);
        assertNotNull(group);
        assertEquals("Address", group.getName());
        assertTrue(group.isCollapsed());
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(1, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());

        group = this.columnGroupHeaderLayer.getGroupByPosition(2);
        assertNotNull(group);
        assertEquals("Facts", group.getName());
        assertTrue(group.isCollapsed());
        assertEquals(8, group.getStartIndex());
        assertEquals(8, group.getVisibleStartIndex());
        assertEquals(2, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());

        group = this.columnGroupHeaderLayer.getGroupByPosition(3);
        assertNotNull(group);
        assertEquals("Personal", group.getName());
        assertTrue(group.isCollapsed());
        assertEquals(11, group.getStartIndex());
        assertEquals(11, group.getVisibleStartIndex());
        assertEquals(3, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());

        // expand all
        this.columnGroupHeaderLayer.expandAllGroups();
        verifyCleanState();
    }

    @Test
    public void shouldLoadStateWithExpandCollapseStates() {
        verifyCleanState();

        Properties properties = new Properties();
        this.gridLayer.saveState("clean", properties);

        // collapse
        this.columnGroupHeaderLayer.collapseGroup("Address");

        this.gridLayer.saveState("one", properties);

        // restore the clean state again
        this.gridLayer.loadState("clean", properties);

        verifyCleanState();

        // load single collapsed
        this.gridLayer.loadState("one", properties);

        assertEquals(11, this.selectionLayer.getColumnCount());

        Group group = this.columnGroupHeaderLayer.getGroupByPosition(4);
        assertNotNull(group);
        assertEquals("Address", group.getName());
        assertTrue(group.isCollapsed());
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());

        // collapse all
        this.columnGroupHeaderLayer.collapseAllGroups();

        this.gridLayer.saveState("all", properties);

        // load single collapsed
        this.gridLayer.loadState("one", properties);

        // verify only Address is collapsed and other groups are not
        // collapsed and in correct state
        assertEquals(11, this.selectionLayer.getColumnCount());

        group = this.columnGroupHeaderLayer.getGroupByPosition(0);
        assertNotNull(group);
        assertEquals("Person", group.getName());
        assertFalse(group.isCollapsed());
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(4, group.getVisibleSpan());

        group = this.columnGroupHeaderLayer.getGroupByPosition(4);
        assertNotNull(group);
        assertEquals("Address", group.getName());
        assertTrue(group.isCollapsed());
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());

        group = this.columnGroupHeaderLayer.getGroupByPosition(5);
        assertNotNull(group);
        assertEquals("Facts", group.getName());
        assertFalse(group.isCollapsed());
        assertEquals(8, group.getStartIndex());
        assertEquals(8, group.getVisibleStartIndex());
        assertEquals(5, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());

        group = this.columnGroupHeaderLayer.getGroupByPosition(8);
        assertNotNull(group);
        assertEquals("Personal", group.getName());
        assertFalse(group.isCollapsed());
        assertEquals(11, group.getStartIndex());
        assertEquals(11, group.getVisibleStartIndex());
        assertEquals(8, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());

        // now load all collapsed again
        this.gridLayer.loadState("all", properties);

        // verify all collapsed
        assertEquals(4, this.columnGroupHeaderLayer.getColumnCount());

        // verify collapsed states
        group = this.columnGroupHeaderLayer.getGroupByPosition(0);
        assertNotNull(group);
        assertEquals("Person", group.getName());
        assertTrue(group.isCollapsed());
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());

        group = this.columnGroupHeaderLayer.getGroupByPosition(1);
        assertNotNull(group);
        assertEquals("Address", group.getName());
        assertTrue(group.isCollapsed());
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(1, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());

        group = this.columnGroupHeaderLayer.getGroupByPosition(2);
        assertNotNull(group);
        assertEquals("Facts", group.getName());
        assertTrue(group.isCollapsed());
        assertEquals(8, group.getStartIndex());
        assertEquals(8, group.getVisibleStartIndex());
        assertEquals(2, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());

        group = this.columnGroupHeaderLayer.getGroupByPosition(3);
        assertNotNull(group);
        assertEquals("Personal", group.getName());
        assertTrue(group.isCollapsed());
        assertEquals(11, group.getStartIndex());
        assertEquals(11, group.getVisibleStartIndex());
        assertEquals(3, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());

        // restore the clean state again
        this.gridLayer.loadState("clean", properties);

        verifyCleanState();
    }

    @Test
    public void shouldDragReorderWithinGroup() {
        this.gridLayer.doCommand(new ColumnReorderStartCommand(this.gridLayer, 2));
        this.gridLayer.doCommand(new ColumnReorderEndCommand(this.gridLayer, 4));

        // no changes in the group header cell
        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(400, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        // no changes in the group
        Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
    }

    @Test
    public void shouldDragReorderFirstColumnWithinGroup() {
        this.gridLayer.doCommand(new ColumnReorderStartCommand(this.gridLayer, 1));
        this.gridLayer.doCommand(new ColumnReorderEndCommand(this.gridLayer, 4));

        // no changes in the group header cell
        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(400, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        // only the visible start index should have changed
        Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(1, group1.getStartIndex());
        assertEquals(1, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
    }

    @Test
    public void shouldDragReorderToFirstColumnWithinGroup() {
        this.gridLayer.doCommand(new ColumnReorderStartCommand(this.gridLayer, 4));
        this.gridLayer.doCommand(new ColumnReorderEndCommand(this.gridLayer, 1));

        // no changes in the group header cell
        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(400, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        // only the visible start index should have changed
        Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(3, group1.getStartIndex());
        assertEquals(3, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
    }

    @Test
    public void shouldDragReorderUngroupLastColumnInGroup() {
        this.gridLayer.doCommand(new ColumnReorderStartCommand(this.gridLayer, 4));
        this.gridLayer.doCommand(new ColumnReorderEndCommand(this.gridLayer, 4));

        // group header cell has less column span
        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(3, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(300, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(3, 0);
        assertEquals(3, cell.getOriginColumnPosition());
        assertEquals(3, cell.getColumnPosition());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(2, cell.getRowSpan());
        assertEquals("Married", cell.getDataValue());
        assertEquals(300, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(100, cell.getBounds().width);
        assertEquals(40, cell.getBounds().height);

        // only the visible start index should have changed
        Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(3, group1.getOriginalSpan());
        assertEquals(3, group1.getVisibleSpan());
    }

    @Test
    public void shouldNotDragReorderUngroupMiddleColumnInGroup() {
        this.gridLayer.doCommand(new ColumnReorderStartCommand(this.gridLayer, 3));
        this.gridLayer.doCommand(new ColumnReorderEndCommand(this.gridLayer, 3));

        // group header cell has not changed
        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(400, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        // group has not changed
        Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
    }

    @Test
    public void shouldDragReorderRightAddUngroupedToGroupAsFirstColumn() {
        this.columnGroupHeaderLayer.removePositionsFromGroup(0, 3);

        // no changes in the group header cell
        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(3, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(300, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(3, 0);
        assertEquals(3, cell.getOriginColumnPosition());
        assertEquals(3, cell.getColumnPosition());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(2, cell.getRowSpan());
        assertEquals("Married", cell.getDataValue());
        assertEquals(300, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(100, cell.getBounds().width);
        assertEquals(40, cell.getBounds().height);

        assertNull(this.columnGroupHeaderLayer.getGroupByPosition(3));

        // only the visible start index should have changed
        Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(3, group1.getOriginalSpan());
        assertEquals(3, group1.getVisibleSpan());

        // drag reorder in same column to add to next group
        this.gridLayer.doCommand(new ColumnReorderStartCommand(this.gridLayer, 4));
        this.gridLayer.doCommand(new ColumnReorderEndCommand(this.gridLayer, 5));

        cell = this.columnGroupHeaderLayer.getCellByPosition(3, 0);
        assertEquals(3, cell.getOriginColumnPosition());
        assertEquals(3, cell.getColumnPosition());
        assertEquals(5, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(300, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(500, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        // only the visible start index should have changed
        group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
        assertEquals(3, group1.getStartIndex());
        assertEquals(3, group1.getVisibleStartIndex());
        assertEquals(3, group1.getVisibleStartPosition());
        assertEquals(5, group1.getOriginalSpan());
        assertEquals(5, group1.getVisibleSpan());
    }

    @Test
    public void shouldDragReorderLeftAddUngroupedToGroupAsLastColumn() {
        this.columnGroupHeaderLayer.removePositionsFromGroup(0, 3);

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(3, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(300, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(3, 0);
        assertEquals(3, cell.getOriginColumnPosition());
        assertEquals(3, cell.getColumnPosition());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(2, cell.getRowSpan());
        assertEquals("Married", cell.getDataValue());
        assertEquals(300, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(100, cell.getBounds().width);
        assertEquals(40, cell.getBounds().height);

        Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(3, group1.getOriginalSpan());
        assertEquals(3, group1.getVisibleSpan());

        // drag reorder in same column to add to next group
        this.gridLayer.doCommand(new ColumnReorderStartCommand(this.gridLayer, 4));
        this.gridLayer.doCommand(new ColumnReorderEndCommand(this.gridLayer, 4));

        cell = this.columnGroupHeaderLayer.getCellByPosition(3, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(3, cell.getColumnPosition());
        assertEquals(4, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(400, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        // only the visible start index should have changed
        group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
    }

    @Test
    public void shouldDragReorderUngroupFirstColumnInGroup() {
        this.gridLayer.doCommand(new ColumnReorderStartCommand(this.gridLayer, 5));
        this.gridLayer.doCommand(new ColumnReorderEndCommand(this.gridLayer, 5));

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
        assertEquals(4, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnPosition());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(2, cell.getRowSpan());
        assertEquals("Street", cell.getDataValue());
        assertEquals(400, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(100, cell.getBounds().width);
        assertEquals(40, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(5, 0);
        assertEquals(5, cell.getOriginColumnPosition());
        assertEquals(5, cell.getColumnPosition());
        assertEquals(3, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(500, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(300, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        assertNull(this.columnGroupHeaderLayer.getGroupByPosition(4));

        Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(5);
        assertEquals(5, group1.getStartIndex());
        assertEquals(5, group1.getVisibleStartIndex());
        assertEquals(5, group1.getVisibleStartPosition());
        assertEquals(3, group1.getOriginalSpan());
        assertEquals(3, group1.getVisibleSpan());
    }

    @Test
    public void shouldDragReorderBetweenGroupsLeft() {
        // second column in second group
        this.gridLayer.doCommand(new ColumnReorderStartCommand(this.gridLayer, 6));
        this.gridLayer.doCommand(new ColumnReorderEndCommand(this.gridLayer, 4));

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(5, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(500, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(5, 0);
        assertEquals(5, cell.getOriginColumnPosition());
        assertEquals(5, cell.getColumnPosition());
        assertEquals(3, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(500, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(300, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        // check group
        Group group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(5, group.getOriginalSpan());
        assertEquals(5, group.getVisibleSpan());

        group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(5);
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(5, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());
    }

    @Test
    public void shouldDragReorderFirstColumnBetweenGroupsLeft() {
        // first column in second group
        this.gridLayer.doCommand(new ColumnReorderStartCommand(this.gridLayer, 5));
        this.gridLayer.doCommand(new ColumnReorderEndCommand(this.gridLayer, 4));

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(5, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(500, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(5, 0);
        assertEquals(5, cell.getOriginColumnPosition());
        assertEquals(5, cell.getColumnPosition());
        assertEquals(3, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(500, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(300, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        // check group
        Group group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(5, group.getOriginalSpan());
        assertEquals(5, group.getVisibleSpan());

        group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(5);
        assertEquals(5, group.getStartIndex());
        assertEquals(5, group.getVisibleStartIndex());
        assertEquals(5, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());
    }

    @Test
    public void shouldDragReorderToFirstColumnBetweenGroupsLeft() {
        // first column in second group
        this.gridLayer.doCommand(new ColumnReorderStartCommand(this.gridLayer, 5));
        this.gridLayer.doCommand(new ColumnReorderEndCommand(this.gridLayer, 1));

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(5, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(500, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(5, 0);
        assertEquals(5, cell.getOriginColumnPosition());
        assertEquals(5, cell.getColumnPosition());
        assertEquals(3, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(500, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(300, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        // check group
        Group group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(5, group.getOriginalSpan());
        assertEquals(5, group.getVisibleSpan());

        group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(5);
        assertEquals(5, group.getStartIndex());
        assertEquals(5, group.getVisibleStartIndex());
        assertEquals(5, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());
    }

    @Test
    public void shouldDragReorderBetweenGroupsRight() {
        // last column in first group
        this.gridLayer.doCommand(new ColumnReorderStartCommand(this.gridLayer, 4));
        // to middle of second group
        this.gridLayer.doCommand(new ColumnReorderEndCommand(this.gridLayer, 7));

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(3, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(300, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(3, 0);
        assertEquals(3, cell.getOriginColumnPosition());
        assertEquals(3, cell.getColumnPosition());
        assertEquals(5, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(300, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(500, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        // check group
        Group group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());

        group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(3, group.getVisibleStartPosition());
        assertEquals(5, group.getOriginalSpan());
        assertEquals(5, group.getVisibleSpan());
    }

    @Test
    public void shouldDragReorderFirstColumnBetweenGroupsRight() {
        // middle column in first group
        this.gridLayer.doCommand(new ColumnReorderStartCommand(this.gridLayer, 3));
        // to first position in second group
        this.gridLayer.doCommand(new ColumnReorderEndCommand(this.gridLayer, 6));

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(3, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(300, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(3, 0);
        assertEquals(3, cell.getOriginColumnPosition());
        assertEquals(3, cell.getColumnPosition());
        assertEquals(5, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(300, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(500, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        // check group
        Group group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());

        group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(3, group.getVisibleStartPosition());
        assertEquals(5, group.getOriginalSpan());
        assertEquals(5, group.getVisibleSpan());
    }

    @Test
    public void shouldDragReorderToFirstColumnBetweenGroupsRight() {
        // first column in first group
        this.gridLayer.doCommand(new ColumnReorderStartCommand(this.gridLayer, 1));
        this.gridLayer.doCommand(new ColumnReorderEndCommand(this.gridLayer, 6));

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(3, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(300, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(3, 0);
        assertEquals(3, cell.getOriginColumnPosition());
        assertEquals(3, cell.getColumnPosition());
        assertEquals(5, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(300, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(500, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        // check group
        Group group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(1, group.getStartIndex());
        assertEquals(1, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());

        group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(3, group.getVisibleStartPosition());
        assertEquals(5, group.getOriginalSpan());
        assertEquals(5, group.getVisibleSpan());
    }

    @Test
    public void shouldReorderWithinGroup() {
        this.gridLayer.doCommand(new ColumnReorderCommand(this.gridLayer, 2, 4));

        // no changes in the group header cell
        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(400, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        // no changes in the group
        Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
    }

    @Test
    public void shouldReorderFirstColumnWithinGroup() {
        this.gridLayer.doCommand(new ColumnReorderCommand(this.gridLayer, 1, 4));

        // no changes in the group header cell
        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(400, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        // only the visible start index should have changed
        Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(1, group1.getStartIndex());
        assertEquals(1, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
    }

    @Test
    public void shouldReorderToFirstColumnWithinGroup() {
        this.gridLayer.doCommand(new ColumnReorderCommand(this.gridLayer, 4, 1));

        // no changes in the group header cell
        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(400, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        // only the visible start index should have changed
        Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(3, group1.getStartIndex());
        assertEquals(3, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
    }

    @Test
    public void shouldReorderUngroupLastColumnInGroup() {
        this.gridLayer.doCommand(new ColumnReorderCommand(this.gridLayer, 4, 4));

        // group header cell has less column span
        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(3, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(300, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(3, 0);
        assertEquals(3, cell.getOriginColumnPosition());
        assertEquals(3, cell.getColumnPosition());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(2, cell.getRowSpan());
        assertEquals("Married", cell.getDataValue());
        assertEquals(300, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(100, cell.getBounds().width);
        assertEquals(40, cell.getBounds().height);

        // only the visible start index should have changed
        Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(3, group1.getOriginalSpan());
        assertEquals(3, group1.getVisibleSpan());
    }

    @Test
    public void shouldNotReorderUngroupMiddleColumnInGroup() {
        this.gridLayer.doCommand(new ColumnReorderCommand(this.gridLayer, 3, 3));

        // group header cell has not changed
        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(400, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        // group has not changed
        Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
    }

    @Test
    public void shouldReorderRightAddUngroupedToGroupAsFirstColumn() {
        this.columnGroupHeaderLayer.removePositionsFromGroup(0, 3);

        // no changes in the group header cell
        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(3, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(300, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(3, 0);
        assertEquals(3, cell.getOriginColumnPosition());
        assertEquals(3, cell.getColumnPosition());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(2, cell.getRowSpan());
        assertEquals("Married", cell.getDataValue());
        assertEquals(300, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(100, cell.getBounds().width);
        assertEquals(40, cell.getBounds().height);

        assertNull(this.columnGroupHeaderLayer.getGroupByPosition(3));

        // only the visible start index should have changed
        Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(3, group1.getOriginalSpan());
        assertEquals(3, group1.getVisibleSpan());

        // drag reorder in same column to add to next group
        this.gridLayer.doCommand(new ColumnReorderCommand(this.gridLayer, 4, 5));

        cell = this.columnGroupHeaderLayer.getCellByPosition(3, 0);
        assertEquals(3, cell.getOriginColumnPosition());
        assertEquals(3, cell.getColumnPosition());
        assertEquals(5, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(300, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(500, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        // only the visible start index should have changed
        group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
        assertEquals(3, group1.getStartIndex());
        assertEquals(3, group1.getVisibleStartIndex());
        assertEquals(3, group1.getVisibleStartPosition());
        assertEquals(5, group1.getOriginalSpan());
        assertEquals(5, group1.getVisibleSpan());
    }

    @Test
    public void shouldReorderLeftAddUngroupedToGroupAsLastColumn() {
        this.columnGroupHeaderLayer.removePositionsFromGroup(0, 3);

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(3, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(300, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(3, 0);
        assertEquals(3, cell.getOriginColumnPosition());
        assertEquals(3, cell.getColumnPosition());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(2, cell.getRowSpan());
        assertEquals("Married", cell.getDataValue());
        assertEquals(300, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(100, cell.getBounds().width);
        assertEquals(40, cell.getBounds().height);

        Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(3, group1.getOriginalSpan());
        assertEquals(3, group1.getVisibleSpan());

        // drag reorder in same column to add to next group
        this.gridLayer.doCommand(new ColumnReorderCommand(this.gridLayer, 4, 4));

        cell = this.columnGroupHeaderLayer.getCellByPosition(3, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(3, cell.getColumnPosition());
        assertEquals(4, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(400, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        // only the visible start index should have changed
        group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
    }

    @Test
    public void shouldReorderUngroupFirstColumnInGroup() {
        this.gridLayer.doCommand(new ColumnReorderCommand(this.gridLayer, 5, 5));

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
        assertEquals(4, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnPosition());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(2, cell.getRowSpan());
        assertEquals("Street", cell.getDataValue());
        assertEquals(400, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(100, cell.getBounds().width);
        assertEquals(40, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(5, 0);
        assertEquals(5, cell.getOriginColumnPosition());
        assertEquals(5, cell.getColumnPosition());
        assertEquals(3, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(500, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(300, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        assertNull(this.columnGroupHeaderLayer.getGroupByPosition(4));

        Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(5);
        assertEquals(5, group1.getStartIndex());
        assertEquals(5, group1.getVisibleStartIndex());
        assertEquals(5, group1.getVisibleStartPosition());
        assertEquals(3, group1.getOriginalSpan());
        assertEquals(3, group1.getVisibleSpan());
    }

    @Test
    public void shouldReorderBetweenGroupsLeft() {
        // second column in second group
        this.gridLayer.doCommand(new ColumnReorderCommand(this.gridLayer, 6, 4));

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(5, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(500, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(5, 0);
        assertEquals(5, cell.getOriginColumnPosition());
        assertEquals(5, cell.getColumnPosition());
        assertEquals(3, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(500, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(300, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        // check group
        Group group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(5, group.getOriginalSpan());
        assertEquals(5, group.getVisibleSpan());

        group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(5);
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(5, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());
    }

    @Test
    public void shouldReorderFirstColumnBetweenGroupsLeft() {
        // first column in second group
        this.gridLayer.doCommand(new ColumnReorderCommand(this.gridLayer, 5, 4));

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(5, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(500, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(5, 0);
        assertEquals(5, cell.getOriginColumnPosition());
        assertEquals(5, cell.getColumnPosition());
        assertEquals(3, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(500, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(300, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        // check group
        Group group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(5, group.getOriginalSpan());
        assertEquals(5, group.getVisibleSpan());

        group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(5);
        assertEquals(5, group.getStartIndex());
        assertEquals(5, group.getVisibleStartIndex());
        assertEquals(5, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());
    }

    @Test
    public void shouldReorderToFirstColumnBetweenGroupsLeft() {
        // first column in second group
        this.gridLayer.doCommand(new ColumnReorderCommand(this.gridLayer, 5, 1));

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(5, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(500, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(5, 0);
        assertEquals(5, cell.getOriginColumnPosition());
        assertEquals(5, cell.getColumnPosition());
        assertEquals(3, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(500, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(300, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        // check group
        Group group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(5, group.getOriginalSpan());
        assertEquals(5, group.getVisibleSpan());

        group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(5);
        assertEquals(5, group.getStartIndex());
        assertEquals(5, group.getVisibleStartIndex());
        assertEquals(5, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());
    }

    @Test
    public void shouldReorderBetweenGroupsRight() {
        // last column in first group
        // to middle of second group
        this.gridLayer.doCommand(new ColumnReorderCommand(this.gridLayer, 4, 7));

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(3, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(300, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(3, 0);
        assertEquals(3, cell.getOriginColumnPosition());
        assertEquals(3, cell.getColumnPosition());
        assertEquals(5, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(300, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(500, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        // check group
        Group group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());

        group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(3, group.getVisibleStartPosition());
        assertEquals(5, group.getOriginalSpan());
        assertEquals(5, group.getVisibleSpan());
    }

    @Test
    public void shouldReorderFirstColumnBetweenGroupsRight() {
        // middle column in first group
        // to first position in second group
        this.gridLayer.doCommand(new ColumnReorderCommand(this.gridLayer, 3, 6));

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(3, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(300, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(3, 0);
        assertEquals(3, cell.getOriginColumnPosition());
        assertEquals(3, cell.getColumnPosition());
        assertEquals(5, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(300, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(500, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        // check group
        Group group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());

        group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(3, group.getVisibleStartPosition());
        assertEquals(5, group.getOriginalSpan());
        assertEquals(5, group.getVisibleSpan());
    }

    @Test
    public void shouldReorderToFirstColumnBetweenGroupsRight() {
        // first column in first group
        this.gridLayer.doCommand(new ColumnReorderCommand(this.gridLayer, 1, 6));

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(3, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(300, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(3, 0);
        assertEquals(3, cell.getOriginColumnPosition());
        assertEquals(3, cell.getColumnPosition());
        assertEquals(5, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(300, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(500, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        // check group
        Group group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(1, group.getStartIndex());
        assertEquals(1, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());

        group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(3, group.getVisibleStartPosition());
        assertEquals(5, group.getOriginalSpan());
        assertEquals(5, group.getVisibleSpan());
    }

    @Test
    public void shouldReorderUngroupedAddColumnToGroupRight() {
        // remove group 1
        this.columnGroupHeaderLayer.removeGroup(0);

        // reorder third column to second group
        this.gridLayer.doCommand(new ColumnReorderCommand(this.gridLayer, 3, 6));

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(0, cell.getColumnIndex());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(2, cell.getRowSpan());
        assertEquals("Firstname", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(100, cell.getBounds().width);
        assertEquals(40, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(1, 0);
        assertEquals(1, cell.getOriginColumnPosition());
        assertEquals(1, cell.getColumnPosition());
        assertEquals(1, cell.getColumnIndex());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(2, cell.getRowSpan());
        assertEquals("Lastname", cell.getDataValue());
        assertEquals(100, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(100, cell.getBounds().width);
        assertEquals(40, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(2, 0);
        assertEquals(2, cell.getOriginColumnPosition());
        assertEquals(2, cell.getColumnPosition());
        assertEquals(3, cell.getColumnIndex());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(2, cell.getRowSpan());
        assertEquals("Married", cell.getDataValue());
        assertEquals(200, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(100, cell.getBounds().width);
        assertEquals(40, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(3, 0);
        assertEquals(3, cell.getOriginColumnPosition());
        assertEquals(3, cell.getColumnPosition());
        assertEquals(4, cell.getColumnIndex());
        assertEquals(5, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(300, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(500, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        Group group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(3, group.getVisibleStartPosition());
        assertEquals(5, group.getOriginalSpan());
        assertEquals(5, group.getVisibleSpan());
    }

    @Test
    public void shouldReorderUngroupedAddColumnToGroupLeft() {
        // remove group 2
        this.columnGroupHeaderLayer.removeGroup(4);

        // reorder fifth column to first group
        this.gridLayer.doCommand(new ColumnReorderCommand(this.gridLayer, 6, 3));

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(0, cell.getColumnIndex());
        assertEquals(5, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(500, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(5, 0);
        assertEquals(5, cell.getOriginColumnPosition());
        assertEquals(5, cell.getColumnPosition());
        assertEquals(4, cell.getColumnIndex());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(2, cell.getRowSpan());
        assertEquals("Street", cell.getDataValue());
        assertEquals(500, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(100, cell.getBounds().width);
        assertEquals(40, cell.getBounds().height);

        Group group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(5, group.getOriginalSpan());
        assertEquals(5, group.getVisibleSpan());
    }

    @Test
    public void shouldReorderUngroupColumnFromGroupLeft() {
        // remove group 1
        this.columnGroupHeaderLayer.removeGroup(0);

        // reorder sixth column in second group to second column
        this.gridLayer.doCommand(new ColumnReorderCommand(this.gridLayer, 6, 3));

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(0, cell.getColumnIndex());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(2, cell.getRowSpan());
        assertEquals("Firstname", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(100, cell.getBounds().width);
        assertEquals(40, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(1, 0);
        assertEquals(1, cell.getOriginColumnPosition());
        assertEquals(1, cell.getColumnPosition());
        assertEquals(1, cell.getColumnIndex());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(2, cell.getRowSpan());
        assertEquals("Lastname", cell.getDataValue());
        assertEquals(100, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(100, cell.getBounds().width);
        assertEquals(40, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(2, 0);
        assertEquals(2, cell.getOriginColumnPosition());
        assertEquals(2, cell.getColumnPosition());
        assertEquals(5, cell.getColumnIndex());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(2, cell.getRowSpan());
        assertEquals("Housenumber", cell.getDataValue());
        assertEquals(200, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(100, cell.getBounds().width);
        assertEquals(40, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(3, 0);
        assertEquals(3, cell.getOriginColumnPosition());
        assertEquals(3, cell.getColumnPosition());
        assertEquals(2, cell.getColumnIndex());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(2, cell.getRowSpan());
        assertEquals("Gender", cell.getDataValue());
        assertEquals(300, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(100, cell.getBounds().width);
        assertEquals(40, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
        assertEquals(4, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnPosition());
        assertEquals(3, cell.getColumnIndex());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(2, cell.getRowSpan());
        assertEquals("Married", cell.getDataValue());
        assertEquals(400, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(100, cell.getBounds().width);
        assertEquals(40, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(5, 0);
        assertEquals(5, cell.getOriginColumnPosition());
        assertEquals(5, cell.getColumnPosition());
        assertEquals(4, cell.getColumnIndex());
        assertEquals(3, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(500, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(300, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        Group group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(5);
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(5, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());
    }

    @Test
    public void shouldReorderUngroupColumnGroupRight() {
        // remove group 2
        this.columnGroupHeaderLayer.removeGroup(4);

        // reorder third column out of group 1
        this.gridLayer.doCommand(new ColumnReorderCommand(this.gridLayer, 3, 7));

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(0, cell.getColumnIndex());
        assertEquals(3, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(300, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(3, 0);
        assertEquals(3, cell.getOriginColumnPosition());
        assertEquals(3, cell.getColumnPosition());
        assertEquals(4, cell.getColumnIndex());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(2, cell.getRowSpan());
        assertEquals("Street", cell.getDataValue());
        assertEquals(300, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(100, cell.getBounds().width);
        assertEquals(40, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(5, 0);
        assertEquals(5, cell.getOriginColumnPosition());
        assertEquals(5, cell.getColumnPosition());
        assertEquals(2, cell.getColumnIndex());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(2, cell.getRowSpan());
        assertEquals("Gender", cell.getDataValue());
        assertEquals(500, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(100, cell.getBounds().width);
        assertEquals(40, cell.getBounds().height);

        Group group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());
    }

    @Test
    public void shouldReorderMultipleUngroupedAddColumnToGroupRight() {
        // remove group 1
        this.columnGroupHeaderLayer.removeGroup(0);

        // reorder first and third column to second group
        this.gridLayer.doCommand(new MultiColumnReorderCommand(this.gridLayer, Arrays.asList(1, 3), 6));

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(1, cell.getColumnIndex());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(2, cell.getRowSpan());
        assertEquals("Lastname", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(100, cell.getBounds().width);
        assertEquals(40, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(1, 0);
        assertEquals(1, cell.getOriginColumnPosition());
        assertEquals(1, cell.getColumnPosition());
        assertEquals(3, cell.getColumnIndex());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(2, cell.getRowSpan());
        assertEquals("Married", cell.getDataValue());
        assertEquals(100, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(100, cell.getBounds().width);
        assertEquals(40, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(2, 0);
        assertEquals(2, cell.getOriginColumnPosition());
        assertEquals(2, cell.getColumnPosition());
        assertEquals(4, cell.getColumnIndex());
        assertEquals(6, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(200, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(600, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        Group group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(2, group.getVisibleStartPosition());
        assertEquals(6, group.getOriginalSpan());
        assertEquals(6, group.getVisibleSpan());
    }

    @Test
    public void shouldReorderMultipleUngroupedAddColumnToGroupLeft() {
        // remove group 2
        this.columnGroupHeaderLayer.removeGroup(4);

        // reorder fifth and seventh column to first group
        this.gridLayer.doCommand(new MultiColumnReorderCommand(this.gridLayer, Arrays.asList(5, 7), 2));

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(0, cell.getColumnIndex());
        assertEquals(6, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(600, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(6, 0);
        assertEquals(6, cell.getOriginColumnPosition());
        assertEquals(6, cell.getColumnPosition());
        assertEquals(5, cell.getColumnIndex());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(2, cell.getRowSpan());
        assertEquals("Housenumber", cell.getDataValue());
        assertEquals(600, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(100, cell.getBounds().width);
        assertEquals(40, cell.getBounds().height);

        Group group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(6, group.getOriginalSpan());
        assertEquals(6, group.getVisibleSpan());
    }

    @Test
    public void shouldReorderMultipleColumnsFromOneGroupToOtherGroupRight() {
        // reorder first and third column to second group
        this.gridLayer.doCommand(new MultiColumnReorderCommand(this.gridLayer, Arrays.asList(1, 3), 6));

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(1, cell.getColumnIndex());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(200, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(2, 0);
        assertEquals(2, cell.getOriginColumnPosition());
        assertEquals(2, cell.getColumnPosition());
        assertEquals(4, cell.getColumnIndex());
        assertEquals(6, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(200, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(600, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(1, group1.getStartIndex());
        assertEquals(1, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(2, group1.getOriginalSpan());
        assertEquals(2, group1.getVisibleSpan());

        Group group2 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(2);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(2, group2.getVisibleStartPosition());
        assertEquals(6, group2.getOriginalSpan());
        assertEquals(6, group2.getVisibleSpan());
    }

    @Test
    public void shouldReorderMultipleColumnsFromOneGroupToOtherGroupLeft() {
        // reorder fifth and seventh column to first group
        this.gridLayer.doCommand(new MultiColumnReorderCommand(this.gridLayer, Arrays.asList(5, 7), 2));

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(0, cell.getColumnIndex());
        assertEquals(6, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(600, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(6, 0);
        assertEquals(6, cell.getOriginColumnPosition());
        assertEquals(6, cell.getColumnPosition());
        assertEquals(5, cell.getColumnIndex());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(600, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(200, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(6, group1.getOriginalSpan());
        assertEquals(6, group1.getVisibleSpan());

        Group group2 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(6);
        assertEquals(5, group2.getStartIndex());
        assertEquals(5, group2.getVisibleStartIndex());
        assertEquals(6, group2.getVisibleStartPosition());
        assertEquals(2, group2.getOriginalSpan());
        assertEquals(2, group2.getVisibleSpan());
    }

    @Test
    public void shouldReorderMultipleColumnsInsideGroupRight() {
        // reorder first two columns in second group
        this.gridLayer.doCommand(new MultiColumnReorderCommand(this.gridLayer, Arrays.asList(5, 6), 9));

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
        assertEquals(4, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnPosition());
        assertEquals(6, cell.getColumnIndex());
        assertEquals(4, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(400, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(400, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        Group group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(6, group.getStartIndex());
        assertEquals(6, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(4, group.getVisibleSpan());
    }

    @Test
    public void shouldReorderMultipleColumnsInsideGroupLeft() {
        // reorder first two columns in second group
        this.gridLayer.doCommand(new MultiColumnReorderCommand(this.gridLayer, Arrays.asList(7, 8), 5));

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
        assertEquals(4, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnPosition());
        assertEquals(6, cell.getColumnIndex());
        assertEquals(4, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(400, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(400, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        Group group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(6, group.getStartIndex());
        assertEquals(6, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(4, group.getVisibleSpan());
    }

    @Test
    public void shouldReorderMultipleColumnsInsideGroupToUngroupRight() {
        // reorder last two columns in second group
        this.gridLayer.doCommand(new MultiColumnReorderCommand(this.gridLayer, Arrays.asList(7, 8), 9));

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
        assertEquals(4, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnPosition());
        assertEquals(4, cell.getColumnIndex());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(400, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(200, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(6, 0);
        assertEquals(6, cell.getOriginColumnPosition());
        assertEquals(6, cell.getColumnPosition());
        assertEquals(6, cell.getColumnIndex());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(2, cell.getRowSpan());
        assertEquals("Postalcode", cell.getDataValue());
        assertEquals(600, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(100, cell.getBounds().width);
        assertEquals(40, cell.getBounds().height);

        Group group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(2, group.getOriginalSpan());
        assertEquals(2, group.getVisibleSpan());

        assertNull(this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(6));
    }

    @Test
    public void shouldReorderMultipleColumnsInsideGroupToUngroupLeft() {
        // reorder first two columns in second group
        this.gridLayer.doCommand(new MultiColumnReorderCommand(this.gridLayer, Arrays.asList(5, 6), 5));

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
        assertEquals(4, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnPosition());
        assertEquals(4, cell.getColumnIndex());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(2, cell.getRowSpan());
        assertEquals("Street", cell.getDataValue());
        assertEquals(400, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(100, cell.getBounds().width);
        assertEquals(40, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(5, 0);
        assertEquals(5, cell.getOriginColumnPosition());
        assertEquals(5, cell.getColumnPosition());
        assertEquals(5, cell.getColumnIndex());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(2, cell.getRowSpan());
        assertEquals("Housenumber", cell.getDataValue());
        assertEquals(500, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(100, cell.getBounds().width);
        assertEquals(40, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(6, 0);
        assertEquals(6, cell.getOriginColumnPosition());
        assertEquals(6, cell.getColumnPosition());
        assertEquals(6, cell.getColumnIndex());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(600, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(200, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        Group group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(6);
        assertEquals(6, group.getStartIndex());
        assertEquals(6, group.getVisibleStartIndex());
        assertEquals(6, group.getVisibleStartPosition());
        assertEquals(2, group.getOriginalSpan());
        assertEquals(2, group.getVisibleSpan());

        assertNull(this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(4));
        assertNull(this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(5));
    }

    @Test
    public void shouldReorderMultipleUngroupedAddColumnToGroupRightOnEdge() {
        // remove group 1
        this.columnGroupHeaderLayer.removeGroup(0);

        // reorder first and third column to second group
        this.gridLayer.doCommand(new MultiColumnReorderCommand(this.gridLayer, Arrays.asList(2, 4), 5));

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(0, cell.getColumnIndex());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(2, cell.getRowSpan());
        assertEquals("Firstname", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(100, cell.getBounds().width);
        assertEquals(40, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(1, 0);
        assertEquals(1, cell.getOriginColumnPosition());
        assertEquals(1, cell.getColumnPosition());
        assertEquals(2, cell.getColumnIndex());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(2, cell.getRowSpan());
        assertEquals("Gender", cell.getDataValue());
        assertEquals(100, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(100, cell.getBounds().width);
        assertEquals(40, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(2, 0);
        assertEquals(2, cell.getOriginColumnPosition());
        assertEquals(2, cell.getColumnPosition());
        assertEquals(1, cell.getColumnIndex());
        assertEquals(6, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(200, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(600, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        Group group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(2);
        assertEquals(1, group.getStartIndex());
        assertEquals(1, group.getVisibleStartIndex());
        assertEquals(2, group.getVisibleStartPosition());
        assertEquals(6, group.getOriginalSpan());
        assertEquals(6, group.getVisibleSpan());
    }

    @Test
    public void shouldReorderMultipleUngroupedAddColumnToGroupLeftOnEdge() {
        // remove group 2
        this.columnGroupHeaderLayer.removeGroup(4);

        // reorder first and third column to second group
        this.gridLayer.doCommand(new MultiColumnReorderCommand(this.gridLayer, Arrays.asList(5, 7), 5));

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(0, cell.getColumnIndex());
        assertEquals(6, cell.getColumnSpan());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(600, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(6, 0);
        assertEquals(6, cell.getOriginColumnPosition());
        assertEquals(6, cell.getColumnPosition());
        assertEquals(5, cell.getColumnIndex());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(2, cell.getRowSpan());
        assertEquals("Housenumber", cell.getDataValue());
        assertEquals(600, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(100, cell.getBounds().width);
        assertEquals(40, cell.getBounds().height);

        Group group = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(6, group.getOriginalSpan());
        assertEquals(6, group.getVisibleSpan());
    }

    @Test
    public void shouldNotBreakUnbreakableGroupOnReorderToUnbreakable() {
        // set second group unbreakable
        this.columnGroupHeaderLayer.setGroupUnbreakable(4, true);

        // try to reorder a column from first group to second
        this.gridLayer.doCommand(new ColumnReorderCommand(this.gridLayer, 2, 5));

        // nothing should have been changed
        this.columnGroupHeaderLayer.setGroupUnbreakable(4, false);
        verifyCleanState();
    }

    @Test
    public void shouldNotBreakUnbreakableGroupOnReorderFromUnbreakable() {
        // set second group unbreakable
        this.columnGroupHeaderLayer.setGroupUnbreakable(4, true);

        // try to reorder a column from second group to first
        this.gridLayer.doCommand(new ColumnReorderCommand(this.gridLayer, 5, 2));

        // nothing should have been changed
        this.columnGroupHeaderLayer.setGroupUnbreakable(4, false);
        verifyCleanState();
    }

    @Test
    public void shouldNotBreakUnbreakableGroupOnDragReorderToUnbreakable() {
        // set second group unbreakable
        this.columnGroupHeaderLayer.setGroupUnbreakable(4, true);

        // try to reorder a column from first group to second
        this.gridLayer.doCommand(new ColumnReorderStartCommand(this.gridLayer, 2));
        this.gridLayer.doCommand(new ColumnReorderEndCommand(this.gridLayer, 6));

        // nothing should have been changed
        this.columnGroupHeaderLayer.setGroupUnbreakable(4, false);
        verifyCleanState();
    }

    @Test
    public void shouldNotBreakUnbreakableGroupOnDragReorderFromUnbreakable() {
        // set second group unbreakable
        this.columnGroupHeaderLayer.setGroupUnbreakable(4, true);

        // try to reorder a column from second group to first
        this.gridLayer.doCommand(new ColumnReorderStartCommand(this.gridLayer, 6));
        this.gridLayer.doCommand(new ColumnReorderEndCommand(this.gridLayer, 2));

        // nothing should have been changed
        this.columnGroupHeaderLayer.setGroupUnbreakable(4, false);
        verifyCleanState();
    }

    @Test
    public void shouldNotBreakUnbreakableGroupOnDragReorderMultipleToUnbreakable() {
        // set second group unbreakable
        this.columnGroupHeaderLayer.setGroupUnbreakable(4, true);

        // try to reorder a column from first group to second
        this.gridLayer.doCommand(new MultiColumnReorderCommand(this.gridLayer, Arrays.asList(2, 3), 6));

        // nothing should have been changed
        this.columnGroupHeaderLayer.setGroupUnbreakable(4, false);
        verifyCleanState();
    }

    @Test
    public void shouldNotBreakUnbreakableGroupOnDragReorderMultipleFromUnbreakable() {
        // set second group unbreakable
        this.columnGroupHeaderLayer.setGroupUnbreakable(4, true);

        // try to reorder a column from second group to first
        this.gridLayer.doCommand(new MultiColumnReorderCommand(this.gridLayer, Arrays.asList(6, 7), 2));

        // nothing should have been changed
        this.columnGroupHeaderLayer.setGroupUnbreakable(4, false);
        verifyCleanState();
    }

    @Test
    public void shouldNotBreakUnbreakableGroupOnReorderToUnbreakableEdgeRight() {
        // set second group unbreakable
        this.columnGroupHeaderLayer.setGroupUnbreakable(4, true);

        // remove first group
        this.columnGroupHeaderLayer.removeGroup(0);

        // try to reorder column 4 to second group
        this.gridLayer.doCommand(new ColumnReorderCommand(this.gridLayer, 4, 5));

        assertNull(this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0));
        assertNull(this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(1));
        assertNull(this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(2));
        assertNull(this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(3));

        Group group2 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());

        Group group3 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(8, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());

        Group group4 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(11, group4.getStartIndex());
        assertEquals(11, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());
    }

    @Test
    public void shouldNotBreakUnbreakableGroupOnReorderToUnbreakableEdgeLeft() {
        // set first group unbreakable
        this.columnGroupHeaderLayer.setGroupUnbreakable(0, true);

        // remove second group
        this.columnGroupHeaderLayer.removeGroup(4);

        // try to reorder column 4 to first group
        this.gridLayer.doCommand(new ColumnReorderCommand(this.gridLayer, 5, 5));

        Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());

        assertNull(this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(4));
        assertNull(this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(5));
        assertNull(this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(6));
        assertNull(this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(7));

        Group group3 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(8, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());

        Group group4 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(11, group4.getStartIndex());
        assertEquals(11, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());
    }

    @Test
    public void shouldNotBreakUnbreakableGroupOnReorderBetweenGroupsRight() {
        // set second group unbreakable
        this.columnGroupHeaderLayer.setGroupUnbreakable(4, true);

        // remove first group
        this.columnGroupHeaderLayer.removeGroup(0);

        // try to reorder column 4 to second group
        this.gridLayer.doCommand(new ColumnReorderCommand(this.gridLayer, 4, 9));

        assertNull(this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0));
        assertNull(this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(1));
        assertNull(this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(2));

        Group group2 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(3, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());

        assertNull(this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(7));

        Group group3 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(8, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());

        Group group4 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(11, group4.getStartIndex());
        assertEquals(11, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());
    }

    @Test
    public void shouldReorderUnbreakableGroupsBetweenGroupsLeft() {
        // set all groups unbreakable
        this.columnGroupHeaderLayer.setGroupUnbreakable(0, true);
        this.columnGroupHeaderLayer.setGroupUnbreakable(4, true);
        this.columnGroupHeaderLayer.setGroupUnbreakable(8, true);
        this.columnGroupHeaderLayer.setGroupUnbreakable(11, true);

        // try to reorder group 3 between group 1 and 2
        this.gridLayer.doCommand(new ColumnGroupReorderCommand(this.gridLayer, 0, 9, 5));

        Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
        assertEquals("Person", group1.getName());

        Group group2 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(8, group2.getStartIndex());
        assertEquals(8, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(3, group2.getOriginalSpan());
        assertEquals(3, group2.getVisibleSpan());
        assertEquals("Facts", group2.getName());

        Group group3 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(4, group3.getStartIndex());
        assertEquals(4, group3.getVisibleStartIndex());
        assertEquals(7, group3.getVisibleStartPosition());
        assertEquals(4, group3.getOriginalSpan());
        assertEquals(4, group3.getVisibleSpan());
        assertEquals("Address", group3.getName());

        Group group4 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(11, group4.getStartIndex());
        assertEquals(11, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());
        assertEquals("Personal", group4.getName());
    }

    @Test
    public void shouldReorderUnbreakableGroupsToStartLeft() {
        // set all groups unbreakable
        this.columnGroupHeaderLayer.setGroupUnbreakable(0, true);
        this.columnGroupHeaderLayer.setGroupUnbreakable(4, true);
        this.columnGroupHeaderLayer.setGroupUnbreakable(8, true);
        this.columnGroupHeaderLayer.setGroupUnbreakable(11, true);

        // try to reorder group 3 to start
        this.gridLayer.doCommand(new ColumnGroupReorderCommand(this.gridLayer, 0, 9, 1));

        Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(8, group1.getStartIndex());
        assertEquals(8, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(3, group1.getOriginalSpan());
        assertEquals(3, group1.getVisibleSpan());
        assertEquals("Facts", group1.getName());

        Group group2 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
        assertEquals(0, group2.getStartIndex());
        assertEquals(0, group2.getVisibleStartIndex());
        assertEquals(3, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());
        assertEquals("Person", group2.getName());

        Group group3 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(4, group3.getStartIndex());
        assertEquals(4, group3.getVisibleStartIndex());
        assertEquals(7, group3.getVisibleStartPosition());
        assertEquals(4, group3.getOriginalSpan());
        assertEquals(4, group3.getVisibleSpan());
        assertEquals("Address", group3.getName());

        Group group4 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(11, group4.getStartIndex());
        assertEquals(11, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());
        assertEquals("Personal", group4.getName());
    }

    @Test
    public void shouldReorderUnbreakableGroupsToEndRight() {
        // increase the client area to show all columns
        this.gridLayer.setClientAreaProvider(new IClientAreaProvider() {

            @Override
            public Rectangle getClientArea() {
                return new Rectangle(0, 0, 1600, 250);
            }

        });
        this.gridLayer.doCommand(new ClientAreaResizeCommand(new Shell(Display.getDefault(), SWT.V_SCROLL | SWT.H_SCROLL)));

        // set all groups unbreakable
        this.columnGroupHeaderLayer.setGroupUnbreakable(0, true);
        this.columnGroupHeaderLayer.setGroupUnbreakable(4, true);
        this.columnGroupHeaderLayer.setGroupUnbreakable(8, true);
        this.columnGroupHeaderLayer.setGroupUnbreakable(11, true);

        // try to reorder group 2 to end
        this.gridLayer.doCommand(new ColumnGroupReorderCommand(this.gridLayer, 0, 5, 15));

        Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
        assertEquals("Person", group1.getName());

        Group group2 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(8, group2.getStartIndex());
        assertEquals(8, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(3, group2.getOriginalSpan());
        assertEquals(3, group2.getVisibleSpan());
        assertEquals("Facts", group2.getName());

        Group group3 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(11, group3.getStartIndex());
        assertEquals(11, group3.getVisibleStartIndex());
        assertEquals(7, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());
        assertEquals("Personal", group3.getName());

        Group group4 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(4, group4.getStartIndex());
        assertEquals(4, group4.getVisibleStartIndex());
        assertEquals(10, group4.getVisibleStartPosition());
        assertEquals(4, group4.getOriginalSpan());
        assertEquals(4, group4.getVisibleSpan());
        assertEquals("Address", group4.getName());
    }

    @Test
    public void shouldReorderUnbreakableGroupsToRight() {
        // increase the client area to show all columns
        this.gridLayer.setClientAreaProvider(new IClientAreaProvider() {

            @Override
            public Rectangle getClientArea() {
                return new Rectangle(0, 0, 1600, 250);
            }

        });
        this.gridLayer.doCommand(new ClientAreaResizeCommand(new Shell(Display.getDefault(), SWT.V_SCROLL | SWT.H_SCROLL)));

        // set all groups unbreakable
        this.columnGroupHeaderLayer.setGroupUnbreakable(0, true);
        this.columnGroupHeaderLayer.setGroupUnbreakable(4, true);
        this.columnGroupHeaderLayer.setGroupUnbreakable(8, true);
        this.columnGroupHeaderLayer.setGroupUnbreakable(11, true);

        // try to reorder group 1 between 2 and 3
        this.gridLayer.doCommand(new ColumnGroupReorderCommand(this.gridLayer, 0, 1, 9));

        Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(4, group1.getStartIndex());
        assertEquals(4, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
        assertEquals("Address", group1.getName());

        Group group2 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(0, group2.getStartIndex());
        assertEquals(0, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());
        assertEquals("Person", group2.getName());

        Group group3 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(8, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());
        assertEquals("Facts", group3.getName());

        Group group4 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(11, group4.getStartIndex());
        assertEquals(11, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());
        assertEquals("Personal", group4.getName());
    }

    @Test
    public void shouldDragReorderEntireColumnGroupToStart() {
        // reorder second group to first
        this.gridLayer.doCommand(new ColumnGroupReorderStartCommand(this.gridLayer, 6, 0));
        this.gridLayer.doCommand(new ColumnGroupReorderEndCommand(this.gridLayer, 1, 0));

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnIndex());
        assertEquals(4, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(400, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
        assertEquals(4, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnIndex());
        assertEquals(4, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(400, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(400, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(4, group1.getStartIndex());
        assertEquals(4, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());

        Group group2 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(0, group2.getStartIndex());
        assertEquals(0, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());
    }

    @Test
    public void shouldReorderEntireColumnGroupToStart() {
        // reorder second group to first
        this.gridLayer.doCommand(new ColumnGroupReorderCommand(this.gridLayer, 0, 5, 1));

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnIndex());
        assertEquals(4, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(400, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
        assertEquals(4, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnIndex());
        assertEquals(4, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(400, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(400, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(4, group1.getStartIndex());
        assertEquals(4, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());

        Group group2 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(0, group2.getStartIndex());
        assertEquals(0, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());
    }

    @Test
    public void shouldDragReorderEntireColumnGroupToLast() {
        // start reorder second group
        this.gridLayer.doCommand(new ColumnGroupReorderStartCommand(this.gridLayer, 6, 0));

        // scroll to show last column
        this.gridLayer.doCommand(new ShowColumnInViewportCommand(13));

        // end reorder to last position
        this.gridLayer.doCommand(new ColumnGroupReorderEndCommand(this.gridLayer, 11, 0));

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(8, cell.getColumnIndex());
        assertEquals(3, cell.getColumnSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(-30, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(300, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(3, 0);
        assertEquals(3, cell.getOriginColumnPosition());
        assertEquals(11, cell.getColumnIndex());
        assertEquals(3, cell.getColumnSpan());
        assertEquals("Personal", cell.getDataValue());
        assertEquals(270, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(300, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(6, 0);
        assertEquals(6, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnIndex());
        assertEquals(4, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(570, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(400, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        // the position is related to the positionLayer, which is the
        // SelectionLayer
        Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
        assertEquals("Person", group1.getName());

        Group group2 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(8, group2.getStartIndex());
        assertEquals(8, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(3, group2.getOriginalSpan());
        assertEquals(3, group2.getVisibleSpan());
        assertEquals("Facts", group2.getName());

        Group group3 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(7);
        assertEquals(11, group3.getStartIndex());
        assertEquals(11, group3.getVisibleStartIndex());
        assertEquals(7, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());
        assertEquals("Personal", group3.getName());

        Group group4 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(10);
        assertEquals(4, group4.getStartIndex());
        assertEquals(4, group4.getVisibleStartIndex());
        assertEquals(10, group4.getVisibleStartPosition());
        assertEquals(4, group4.getOriginalSpan());
        assertEquals(4, group4.getVisibleSpan());
        assertEquals("Address", group4.getName());
    }

    @Test
    public void shouldReorderEntireColumnGroupToLast() {
        // reorder second group to first
        this.gridLayer.doCommand(new ColumnGroupReorderCommand(this.gridLayer, 0, 5, 1));

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnIndex());
        assertEquals(4, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(400, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
        assertEquals(4, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnIndex());
        assertEquals(4, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(400, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(400, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(4, group1.getStartIndex());
        assertEquals(4, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());

        Group group2 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(0, group2.getStartIndex());
        assertEquals(0, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());
    }

    @Test
    public void shouldReorderEntireColumnGroupBetweenOtherGroups() {
        // reorder third group between first and second first
        this.gridLayer.doCommand(new ColumnGroupReorderCommand(this.gridLayer, 0, 9, 5));

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnIndex());
        assertEquals(4, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(400, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(4, 0);
        assertEquals(4, cell.getOriginColumnPosition());
        assertEquals(8, cell.getColumnIndex());
        assertEquals(3, cell.getColumnSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(400, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(300, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        cell = this.columnGroupHeaderLayer.getCellByPosition(7, 0);
        assertEquals(7, cell.getOriginColumnPosition());
        assertEquals(4, cell.getColumnIndex());
        assertEquals(4, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(700, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(400, cell.getBounds().width);
        assertEquals(20, cell.getBounds().height);

        Group group1 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());

        Group group2 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(8, group2.getStartIndex());
        assertEquals(8, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(3, group2.getOriginalSpan());
        assertEquals(3, group2.getVisibleSpan());

        Group group3 = this.columnGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(4, group3.getStartIndex());
        assertEquals(4, group3.getVisibleStartIndex());
        assertEquals(7, group3.getVisibleStartPosition());
        assertEquals(4, group3.getOriginalSpan());
        assertEquals(4, group3.getVisibleSpan());
    }

    @Test
    public void shouldNotUngroupOnReorderEntireGroupToGroupStart() {
        this.gridLayer.doCommand(new ColumnGroupReorderCommand(this.gridLayer, 0, 5, 5));
        verifyCleanState();
    }

    @Test
    public void shouldNotUngroupOnReorderEntireGroupToGroupEnd() {
        this.gridLayer.doCommand(new ColumnGroupReorderCommand(this.gridLayer, 0, 6, 9));
        verifyCleanState();
    }

    // TODO reordering with expand/collapse
    // TODO reordering with hidden

    // TODO testcases for dynamic grouping/ungrouping

    @Test
    public void shouldReturnConfigLabels() {
        // check expanded column group
        LabelStack stack = this.columnGroupHeaderLayer.getConfigLabelsByPosition(0, 0);
        assertEquals(2, stack.getLabels().size());
        assertTrue(stack.hasLabel(GridRegion.COLUMN_GROUP_HEADER));
        assertTrue(stack.hasLabel(DefaultColumnGroupHeaderLayerConfiguration.GROUP_EXPANDED_CONFIG_TYPE));

        // check collapsed column group
        this.columnGroupHeaderLayer.collapseGroup(0);
        stack = this.columnGroupHeaderLayer.getConfigLabelsByPosition(0, 0);
        assertEquals(2, stack.getLabels().size());
        assertTrue(stack.hasLabel(GridRegion.COLUMN_GROUP_HEADER));
        assertTrue(stack.hasLabel(DefaultColumnGroupHeaderLayerConfiguration.GROUP_COLLAPSED_CONFIG_TYPE));

        // expand again as positions are visible and otherwise we cannot remove
        // a column from the group
        this.columnGroupHeaderLayer.expandGroup(0);

        // remove last column from first group
        this.columnGroupHeaderLayer.removePositionsFromGroup(0, 3);

        // check ungrouped
        stack = this.columnGroupHeaderLayer.getConfigLabelsByPosition(3, 0);
        assertEquals(0, stack.getLabels().size());
    }

    @Test
    public void shouldReturnConfigLabelsWithAccumulator() {
        // set config label accumulator
        this.columnGroupHeaderLayer.setConfigLabelAccumulator(new IConfigLabelAccumulator() {

            @Override
            public void accumulateConfigLabels(LabelStack configLabels, int columnPosition, int rowPosition) {
                if (columnPosition == 0 || columnPosition == 3) {
                    configLabels.addLabel("custom");
                }
            }
        });

        // check expanded column group
        LabelStack stack = this.columnGroupHeaderLayer.getConfigLabelsByPosition(0, 0);
        assertEquals(3, stack.getLabels().size());
        assertTrue(stack.hasLabel(GridRegion.COLUMN_GROUP_HEADER));
        assertTrue(stack.hasLabel("custom"));
        assertTrue(stack.hasLabel(DefaultColumnGroupHeaderLayerConfiguration.GROUP_EXPANDED_CONFIG_TYPE));

        // check collapsed column group
        this.columnGroupHeaderLayer.collapseGroup(0);
        stack = this.columnGroupHeaderLayer.getConfigLabelsByPosition(0, 0);
        assertEquals(3, stack.getLabels().size());
        assertTrue(stack.hasLabel(GridRegion.COLUMN_GROUP_HEADER));
        assertTrue(stack.hasLabel("custom"));
        assertTrue(stack.hasLabel(DefaultColumnGroupHeaderLayerConfiguration.GROUP_COLLAPSED_CONFIG_TYPE));

        // expand again as positions are visible and otherwise we cannot remove
        // a column from the group
        this.columnGroupHeaderLayer.expandGroup(0);

        // remove last column from first group
        this.columnGroupHeaderLayer.removePositionsFromGroup(0, 3);

        // check ungrouped
        stack = this.columnGroupHeaderLayer.getConfigLabelsByPosition(3, 0);
        assertEquals(0, stack.getLabels().size());
    }

    @Test
    public void shouldCalculateRowHeightByPosition() {
        this.columnGroupHeaderLayer.clearAllGroups();
        this.columnGroupHeaderLayer.setRowHeight(100);
        // Height of the header column row - see fixture
        assertEquals(120, this.columnGroupHeaderLayer.getHeight());
        assertEquals(2, this.columnGroupHeaderLayer.getRowCount());
        assertEquals(100, this.columnGroupHeaderLayer.getRowHeightByPosition(0));
        assertEquals(20, this.columnGroupHeaderLayer.getRowHeightByPosition(1));
        // Test calculated height
        this.columnGroupHeaderLayer.setCalculateHeight(true);
        assertEquals(20, this.columnGroupHeaderLayer.getHeight());
        assertEquals(2, this.columnGroupHeaderLayer.getRowCount());
        assertEquals(0, this.columnGroupHeaderLayer.getRowHeightByPosition(0));
        assertEquals(20, this.columnGroupHeaderLayer.getRowHeightByPosition(1));
    }

    @Test
    public void shouldCalculateRowHeightOnGroupModelChanges() {
        this.columnGroupHeaderLayer.setCalculateHeight(true);

        assertEquals(40, this.columnGroupHeaderLayer.getHeight());
        assertEquals(2, this.columnGroupHeaderLayer.getRowCount());

        ILayerCell cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(4, cell.getColumnSpan());
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());

        cell = this.columnGroupHeaderLayer.getCellByPosition(0, 1);
        assertEquals("Firstname", cell.getDataValue());
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(1, cell.getOriginRowPosition());
        assertEquals(1, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());

        this.columnGroupHeaderLayer.clearAllGroups();

        assertEquals(20, this.columnGroupHeaderLayer.getHeight());
        assertEquals(2, this.columnGroupHeaderLayer.getRowCount());
        assertEquals(0, this.columnGroupHeaderLayer.getRowHeightByPosition(0));
        assertEquals(20, this.columnGroupHeaderLayer.getRowHeightByPosition(1));

        cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals("Firstname", cell.getDataValue());
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(2, cell.getRowSpan());

        this.columnGroupHeaderLayer.setCalculateHeight(false);

        assertEquals(40, this.columnGroupHeaderLayer.getHeight());
        assertEquals(2, this.columnGroupHeaderLayer.getRowCount());

        cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals("Firstname", cell.getDataValue());
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(2, cell.getRowSpan());

        cell = this.columnGroupHeaderLayer.getCellByPosition(0, 1);
        assertEquals("Firstname", cell.getDataValue());
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(1, cell.getRowPosition());
        assertEquals(2, cell.getRowSpan());

        // add group again
        this.columnGroupHeaderLayer.addGroup("Person", 0, 4);

        assertEquals(40, this.columnGroupHeaderLayer.getHeight());
        assertEquals(2, this.columnGroupHeaderLayer.getRowCount());

        cell = this.columnGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(4, cell.getColumnSpan());
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());

        cell = this.columnGroupHeaderLayer.getCellByPosition(0, 1);
        assertEquals("Firstname", cell.getDataValue());
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(1, cell.getColumnSpan());
        assertEquals(1, cell.getOriginRowPosition());
        assertEquals(1, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
    }

    // TODO testcases with compositions that have no scrolling
    // TODO testcases with hierarchical tree layer
    // TODO testcases with freeze composition
}
