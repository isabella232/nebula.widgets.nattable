/*******************************************************************************
 * Copyright (c) 2013 Dirk Fauth and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Dirk Fauth <dirk.fauth@gmail.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.nebula.widgets.nattable.examples._800_Integration;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.command.VisualRefreshCommand;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.ConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.DefaultNatTableStyleConfiguration;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.ExtendedReflectiveColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.data.IColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.data.convert.DefaultDoubleDisplayConverter;
import org.eclipse.nebula.widgets.nattable.examples.AbstractNatExample;
import org.eclipse.nebula.widgets.nattable.examples.data.person.Address;
import org.eclipse.nebula.widgets.nattable.examples.data.person.ExtendedPersonWithAddress;
import org.eclipse.nebula.widgets.nattable.examples.data.person.Person;
import org.eclipse.nebula.widgets.nattable.examples.data.person.Person.Gender;
import org.eclipse.nebula.widgets.nattable.examples.data.person.PersonService;
import org.eclipse.nebula.widgets.nattable.examples.runner.StandaloneNatExampleRunner;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsEventLayer;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsSortModel;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.filterrow.DefaultGlazedListsFilterStrategy;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.groupBy.GroupByConfigAttributes;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.groupBy.GroupByDataLayer;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.groupBy.GroupByHeaderLayer;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.groupBy.GroupByHeaderMenuConfiguration;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.groupBy.GroupByModel;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.groupBy.summary.IGroupBySummaryProvider;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.groupBy.summary.SummationGroupBySummaryProvider;
import org.eclipse.nebula.widgets.nattable.filterrow.FilterRowHeaderComposite;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultColumnHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultCornerDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultRowHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.layer.ColumnHeaderLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.CornerLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.DefaultColumnHeaderDataLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.DefaultRowHeaderDataLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.GridLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.RowHeaderLayer;
import org.eclipse.nebula.widgets.nattable.hideshow.ColumnHideShowLayer;
import org.eclipse.nebula.widgets.nattable.layer.AbstractLayerTransform;
import org.eclipse.nebula.widgets.nattable.layer.CompositeLayer;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.layer.cell.ColumnLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.painter.cell.CheckBoxPainter;
import org.eclipse.nebula.widgets.nattable.persistence.command.DisplayPersistenceDialogCommandHandler;
import org.eclipse.nebula.widgets.nattable.reorder.ColumnReorderLayer;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.sort.SortHeaderLayer;
import org.eclipse.nebula.widgets.nattable.sort.config.SingleClickSortConfiguration;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.style.Style;
import org.eclipse.nebula.widgets.nattable.summaryrow.SummaryDisplayConverter;
import org.eclipse.nebula.widgets.nattable.tree.TreeLayer;
import org.eclipse.nebula.widgets.nattable.tree.command.TreeCollapseAllCommand;
import org.eclipse.nebula.widgets.nattable.tree.command.TreeExpandAllCommand;
import org.eclipse.nebula.widgets.nattable.ui.menu.AbstractHeaderMenuConfiguration;
import org.eclipse.nebula.widgets.nattable.ui.menu.PopupMenuBuilder;
import org.eclipse.nebula.widgets.nattable.util.GUIHelper;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TransformedList;

/**
 * Simple example showing how to add the group by feature to the layer
 * composition of a grid in conjunction with showing summary values of
 * groupings.
 *
 * @author Dirk Fauth
 *
 */
public class _808_SortableGroupByWithFilterExample extends AbstractNatExample {

    private IGroupBySummaryProvider<ExtendedPersonWithAddress> sumMoneySummaryProvider;
    private IGroupBySummaryProvider<ExtendedPersonWithAddress> avgMoneySummaryProvider;

    private boolean useMoneySum = true;

    public static void main(String[] args) throws Exception {
        StandaloneNatExampleRunner
                .run(new _808_SortableGroupByWithFilterExample());
    }

    @Override
    public String getDescription() {
        return "This example shows the usage of the group by feature in conjunction with summary values of the groupings.";
    }

    @Override
    public Control createExampleControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout());

        // create a new ConfigRegistry which will be needed for GlazedLists
        // handling
        final ConfigRegistry configRegistry = new ConfigRegistry();

        // property names of the ExtendedPersonWithAddress class
        String[] propertyNames = { "firstName", "lastName", "age", "money",
                "married", "gender", "birthday" };

        // mapping from property to label, needed for column header labels
        Map<String, String> propertyToLabelMap = new HashMap<String, String>();
        propertyToLabelMap.put("firstName", "Firstname");
        propertyToLabelMap.put("lastName", "Lastname");
        propertyToLabelMap.put("age", "Age");
        propertyToLabelMap.put("money", "Money");
        propertyToLabelMap.put("married", "Married");
        propertyToLabelMap.put("gender", "Gender");
        propertyToLabelMap.put("birthday", "Birthday");

        final IColumnPropertyAccessor<ExtendedPersonWithAddress> columnPropertyAccessor = new ExtendedReflectiveColumnPropertyAccessor<ExtendedPersonWithAddress>(
                propertyNames);

        // to enable the group by summary feature, the GroupByDataLayer needs to
        // know the ConfigRegistry
        final BodyLayerStack<ExtendedPersonWithAddress> bodyLayerStack = new BodyLayerStack<ExtendedPersonWithAddress>(
                PersonService.getExtendedPersonsWithAddress(10000),
                columnPropertyAccessor, configRegistry);

        bodyLayerStack.getBodyDataLayer().setConfigLabelAccumulator(
                new ColumnLabelAccumulator());

        // build the column header layer
        IDataProvider columnHeaderDataProvider = new DefaultColumnHeaderDataProvider(
                propertyNames, propertyToLabelMap);
        DataLayer columnHeaderDataLayer = new DefaultColumnHeaderDataLayer(
                columnHeaderDataProvider);
        ILayer columnHeaderLayer = new ColumnHeaderLayer(columnHeaderDataLayer,
                bodyLayerStack, bodyLayerStack.getSelectionLayer());

        // add sorting
        SortHeaderLayer<ExtendedPersonWithAddress> sortHeaderLayer = new SortHeaderLayer<ExtendedPersonWithAddress>(
                columnHeaderLayer,
                new GlazedListsSortModel<ExtendedPersonWithAddress>(
                        bodyLayerStack.getSortedList(), columnPropertyAccessor,
                        configRegistry, columnHeaderDataLayer), false);

        // connect sortModel to GroupByDataLayer to support sorting by group by
        // summary values
        bodyLayerStack.getBodyDataLayer().setSortModel(
                sortHeaderLayer.getSortModel());

        // add the filter row functionality
        final FilterRowHeaderComposite<ExtendedPersonWithAddress> filterRowHeaderLayer = new FilterRowHeaderComposite<ExtendedPersonWithAddress>(
                new DefaultGlazedListsFilterStrategy<ExtendedPersonWithAddress>(
                        bodyLayerStack.getFilterList(), columnPropertyAccessor,
                        configRegistry), sortHeaderLayer, columnHeaderDataLayer
                        .getDataProvider(), configRegistry);

        // build the row header layer
        IDataProvider rowHeaderDataProvider = new DefaultRowHeaderDataProvider(
                bodyLayerStack.getBodyDataProvider());
        DataLayer rowHeaderDataLayer = new DefaultRowHeaderDataLayer(
                rowHeaderDataProvider);
        ILayer rowHeaderLayer = new RowHeaderLayer(rowHeaderDataLayer,
                bodyLayerStack, bodyLayerStack.getSelectionLayer());

        // build the corner layer
        IDataProvider cornerDataProvider = new DefaultCornerDataProvider(
                columnHeaderDataProvider, rowHeaderDataProvider);
        DataLayer cornerDataLayer = new DataLayer(cornerDataProvider);
        ILayer cornerLayer = new CornerLayer(cornerDataLayer, rowHeaderLayer,
                filterRowHeaderLayer);

        // build the grid layer
        GridLayer gridLayer = new GridLayer(bodyLayerStack,
                filterRowHeaderLayer, rowHeaderLayer, cornerLayer);

        // set the group by header on top of the grid
        CompositeLayer compositeGridLayer = new CompositeLayer(1, 2);
        final GroupByHeaderLayer groupByHeaderLayer = new GroupByHeaderLayer(
                bodyLayerStack.getGroupByModel(), gridLayer,
                columnHeaderDataProvider);
        compositeGridLayer.setChildLayer(GroupByHeaderLayer.GROUP_BY_REGION,
                groupByHeaderLayer, 0, 0);
        compositeGridLayer.setChildLayer("Grid", gridLayer, 0, 1);

        // turn the auto configuration off as we want to add our header menu
        // configuration
        final NatTable natTable = new NatTable(container, compositeGridLayer,
                false);

        // as the autoconfiguration of the NatTable is turned off, we have to
        // add the
        // DefaultNatTableStyleConfiguration and the ConfigRegistry manually
        natTable.setConfigRegistry(configRegistry);
        natTable.addConfiguration(new DefaultNatTableStyleConfiguration());

        // add some additional styling
        natTable.addConfiguration(new AbstractRegistryConfiguration() {

            @Override
            public void configureRegistry(IConfigRegistry configRegistry) {
                configRegistry.registerConfigAttribute(
                        CellConfigAttributes.CELL_PAINTER,
                        new CheckBoxPainter(), DisplayMode.NORMAL,
                        ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + 4);

                configRegistry.registerConfigAttribute(
                        CellConfigAttributes.DISPLAY_CONVERTER,
                        new DefaultDoubleDisplayConverter(),
                        DisplayMode.NORMAL,
                        ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + 3);
            }
        });

        // add sorting configuration
        natTable.addConfiguration(new SingleClickSortConfiguration());

        this.sumMoneySummaryProvider = new SummationGroupBySummaryProvider<ExtendedPersonWithAddress>(
                columnPropertyAccessor);
        this.avgMoneySummaryProvider = new AverageMoneyGroupBySummaryProvider();

        // add group by summary configuration
        natTable.addConfiguration(new AbstractRegistryConfiguration() {

            @Override
            public void configureRegistry(IConfigRegistry configRegistry) {
                configRegistry.registerConfigAttribute(
                        GroupByConfigAttributes.GROUP_BY_SUMMARY_PROVIDER,
                        _808_SortableGroupByWithFilterExample.this.sumMoneySummaryProvider, DisplayMode.NORMAL,
                        GroupByDataLayer.GROUP_BY_COLUMN_PREFIX + 3);

                configRegistry.registerConfigAttribute(
                        GroupByConfigAttributes.GROUP_BY_SUMMARY_PROVIDER,
                        new AverageAgeGroupBySummaryProvider(),
                        DisplayMode.NORMAL,
                        GroupByDataLayer.GROUP_BY_COLUMN_PREFIX + 2);

                configRegistry.registerConfigAttribute(
                        GroupByConfigAttributes.GROUP_BY_CHILD_COUNT_PATTERN,
                        "[{0}] - ({1})");

                configRegistry.registerConfigAttribute(
                        GroupByConfigAttributes.GROUP_BY_HINT,
                        "Drag columns here");

                Style hintStyle = new Style();
                hintStyle.setAttributeValue(CellStyleAttributes.FONT, GUIHelper
                        .getFont(new FontData("Arial", 10, SWT.ITALIC)));
                configRegistry.registerConfigAttribute(
                        GroupByConfigAttributes.GROUP_BY_HINT_STYLE, hintStyle);

                // register a groupBy double display converter to avoid
                // rendering rounding issues
                configRegistry.registerConfigAttribute(
                        CellConfigAttributes.DISPLAY_CONVERTER,
                        new SummaryDisplayConverter(
                                new DefaultDoubleDisplayConverter()),
                        DisplayMode.NORMAL,
                        GroupByDataLayer.GROUP_BY_SUMMARY_COLUMN_PREFIX + 3);
            }
        });

        // add group by header configuration
        natTable.addConfiguration(new GroupByHeaderMenuConfiguration(natTable,
                groupByHeaderLayer));

        natTable.addConfiguration(new AbstractHeaderMenuConfiguration(natTable) {

            @Override
            protected PopupMenuBuilder createColumnHeaderMenu(NatTable natTable) {
                return super.createColumnHeaderMenu(natTable)
                        .withHideColumnMenuItem().withShowAllColumnsMenuItem()
                        .withStateManagerMenuItemProvider();
            }

            @Override
            protected PopupMenuBuilder createCornerMenu(NatTable natTable) {
                return super.createCornerMenu(natTable)
                        .withShowAllColumnsMenuItem()
                        .withStateManagerMenuItemProvider();
            }
        });

        natTable.configure();

        natTable.registerCommandHandler(new DisplayPersistenceDialogCommandHandler(
                natTable));

        GridDataFactory.fillDefaults().grab(true, true).applyTo(natTable);

        Composite buttonPanel = new Composite(container, SWT.NONE);
        buttonPanel.setLayout(new RowLayout());
        GridDataFactory.fillDefaults().grab(true, false).applyTo(buttonPanel);

        Button toggleHeaderButton = new Button(buttonPanel, SWT.PUSH);
        toggleHeaderButton.setText("Toggle Group By Header");
        toggleHeaderButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                groupByHeaderLayer.setVisible(!groupByHeaderLayer.isVisible());
            }
        });

        Button toggleFilterButton = new Button(buttonPanel, SWT.PUSH);
        toggleFilterButton.setText("Toggle Filter Row");
        toggleFilterButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                filterRowHeaderLayer.setFilterRowVisible(!filterRowHeaderLayer
                        .isFilterRowVisible());
            }
        });

        Button collapseAllButton = new Button(buttonPanel, SWT.PUSH);
        collapseAllButton.setText("Collapse All");
        collapseAllButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                natTable.doCommand(new TreeCollapseAllCommand());
            }
        });

        Button expandAllButton = new Button(buttonPanel, SWT.PUSH);
        expandAllButton.setText("Expand All");
        expandAllButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                natTable.doCommand(new TreeExpandAllCommand());
            }
        });

        Button toggleMoneySummaryButton = new Button(buttonPanel, SWT.PUSH);
        toggleMoneySummaryButton
                .setText("Toggle Money Group Summary (SUM/AVG)");
        toggleMoneySummaryButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                // clear the group by summary cache so the new summary
                // calculation gets triggered
                bodyLayerStack.getBodyDataLayer().clearCache();

                _808_SortableGroupByWithFilterExample.this.useMoneySum = !_808_SortableGroupByWithFilterExample.this.useMoneySum;
                if (_808_SortableGroupByWithFilterExample.this.useMoneySum) {
                    configRegistry.registerConfigAttribute(
                            GroupByConfigAttributes.GROUP_BY_SUMMARY_PROVIDER,
                            _808_SortableGroupByWithFilterExample.this.sumMoneySummaryProvider, DisplayMode.NORMAL,
                            GroupByDataLayer.GROUP_BY_COLUMN_PREFIX + 3);
                } else {
                    configRegistry.registerConfigAttribute(
                            GroupByConfigAttributes.GROUP_BY_SUMMARY_PROVIDER,
                            _808_SortableGroupByWithFilterExample.this.avgMoneySummaryProvider, DisplayMode.NORMAL,
                            GroupByDataLayer.GROUP_BY_COLUMN_PREFIX + 3);
                }
                natTable.doCommand(new VisualRefreshCommand());
            }
        });

        // this button adds data to the grid
        // try to group by last name, sort by last name desc and then add
        // dynamic data for verification
        Button addDynamicDataButton = new Button(buttonPanel, SWT.PUSH);
        addDynamicDataButton.setText("Add Data");
        addDynamicDataButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Address address = new Address();
                address.setStreet("Some Street");
                address.setHousenumber(42);
                address.setPostalCode(12345);
                address.setCity("In the clouds");

                Person person = new Person(42, "Ralph", "Wiggum", Gender.MALE,
                        false, new Date());
                ExtendedPersonWithAddress entry = new ExtendedPersonWithAddress(
                        person, address, "0000", "The little Ralphy", 0,
                        new ArrayList<String>(), new ArrayList<String>());
                bodyLayerStack.getEventList().add(entry);

                person = new Person(42, "Clancy", "Wiggum", Gender.MALE, true,
                        new Date());
                entry = new ExtendedPersonWithAddress(person, address, "XXXL",
                        "It is Chief Wiggum", 0, new ArrayList<String>(),
                        new ArrayList<String>());
                bodyLayerStack.getEventList().add(entry);

                person = new Person(42, "Sarah", "Wiggum", Gender.FEMALE, true,
                        new Date());
                entry = new ExtendedPersonWithAddress(person, address, "mommy",
                        "Little Ralphy's mother", 0, new ArrayList<String>(),
                        new ArrayList<String>());
                bodyLayerStack.getEventList().add(entry);
            }
        });

        return container;
    }

    /**
     * Always encapsulate the body layer stack in an AbstractLayerTransform to
     * ensure that the index transformations are performed in later commands.
     *
     * @param <T>
     */
    class BodyLayerStack<T> extends AbstractLayerTransform {

        private final EventList<T> eventList;
        private final SortedList<T> sortedList;
        private final FilterList<T> filterList;

        private final IDataProvider bodyDataProvider;

        private final GroupByDataLayer<T> bodyDataLayer;

        private final SelectionLayer selectionLayer;

        private final GroupByModel groupByModel = new GroupByModel();

        public BodyLayerStack(List<T> values,
                IColumnPropertyAccessor<T> columnPropertyAccessor,
                ConfigRegistry configRegistry) {
            // wrapping of the list to show into GlazedLists
            // see http://publicobject.com/glazedlists/ for further information
            this.eventList = GlazedLists.eventList(values);
            TransformedList<T, T> rowObjectsGlazedList = GlazedLists
                    .threadSafeList(this.eventList);

            // use the SortedList constructor with 'null' for the Comparator
            // because the Comparator
            // will be set by configuration
            this.sortedList = new SortedList<T>(rowObjectsGlazedList, null);
            // wrap the SortedList with the FilterList
            this.filterList = new FilterList<T>(this.sortedList);

            // Use the GroupByDataLayer instead of the default DataLayer
            this.bodyDataLayer = new GroupByDataLayer<T>(getGroupByModel(),
                    this.filterList, columnPropertyAccessor, configRegistry);
            // get the IDataProvider that was created by the GroupByDataLayer
            this.bodyDataProvider = this.bodyDataLayer.getDataProvider();

            // layer for event handling of GlazedLists and PropertyChanges
            GlazedListsEventLayer<T> glazedListsEventLayer = new GlazedListsEventLayer<T>(
                    this.bodyDataLayer, this.filterList);

            ColumnReorderLayer columnReorderLayer = new ColumnReorderLayer(
                    glazedListsEventLayer);
            ColumnHideShowLayer columnHideShowLayer = new ColumnHideShowLayer(
                    columnReorderLayer);
            this.selectionLayer = new SelectionLayer(columnHideShowLayer);

            // add a tree layer to visualise the grouping
            TreeLayer treeLayer = new TreeLayer(this.selectionLayer,
                    this.bodyDataLayer.getTreeRowModel());

            ViewportLayer viewportLayer = new ViewportLayer(treeLayer);

            setUnderlyingLayer(viewportLayer);
        }

        public SelectionLayer getSelectionLayer() {
            return this.selectionLayer;
        }

        public EventList<T> getEventList() {
            return this.eventList;
        }

        public SortedList<T> getSortedList() {
            return this.sortedList;
        }

        public FilterList<T> getFilterList() {
            return this.filterList;
        }

        public IDataProvider getBodyDataProvider() {
            return this.bodyDataProvider;
        }

        public GroupByDataLayer<T> getBodyDataLayer() {
            return this.bodyDataLayer;
        }

        public GroupByModel getGroupByModel() {
            return this.groupByModel;
        }
    }

    /**
     * Example implementation for a typed IGroupBySummaryProvider that
     * calculates the average age of ExtendedPersonWithAddress objects in a
     * grouping.
     */
    class AverageAgeGroupBySummaryProvider implements
            IGroupBySummaryProvider<ExtendedPersonWithAddress> {

        @Override
        public Object summarize(int columnIndex,
                List<ExtendedPersonWithAddress> children) {
            int summaryValue = 0;
            for (ExtendedPersonWithAddress child : children) {
                summaryValue += child.getAge();
            }
            return summaryValue / (children.size() > 0 ? children.size() : 1);
        }

    }

    /**
     * Example implementation for a typed IGroupBySummaryProvider that
     * calculates the average money of ExtendedPersonWithAddress objects in a
     * grouping.
     */
    class AverageMoneyGroupBySummaryProvider implements
            IGroupBySummaryProvider<ExtendedPersonWithAddress> {

        @Override
        public Object summarize(int columnIndex,
                List<ExtendedPersonWithAddress> children) {
            int summaryValue = 0;
            for (ExtendedPersonWithAddress child : children) {
                summaryValue += child.getMoney();
            }
            return summaryValue / (children.size() > 0 ? children.size() : 1);
        }

    }
}
