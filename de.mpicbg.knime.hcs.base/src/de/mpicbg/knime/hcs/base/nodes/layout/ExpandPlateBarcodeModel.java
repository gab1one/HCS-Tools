package de.mpicbg.knime.hcs.base.nodes.layout;

import de.mpicbg.knime.hcs.base.HCSToolsBundleActivator;
import de.mpicbg.knime.hcs.base.nodes.norm.AbstractScreenTrafoModel;
import de.mpicbg.knime.hcs.base.prefs.HCSToolsPreferenceInitializer;
import de.mpicbg.knime.knutils.AbstractNodeModel;
import de.mpicbg.knime.knutils.Attribute;
import de.mpicbg.knime.knutils.AttributeUtils;
import de.mpicbg.knime.knutils.InputTableAttribute;
import de.mpicbg.knime.knutils.TableUpdateCache;
import de.mpicbg.knime.hcs.core.Utils;
import de.mpicbg.knime.hcs.core.barcodes.BarcodeParser;
import de.mpicbg.knime.hcs.core.barcodes.BarcodeParserFactory;
import de.mpicbg.knime.hcs.core.barcodes.namedregexp.NamedPattern;
import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.data.DataColumnSpec;

import java.util.*;
import java.util.regex.Matcher;


public class ExpandPlateBarcodeModel extends AbstractNodeModel {
	
	public static final String CFG_BARCODE_COLUMN = "barcode.column";
	public static final String CFG_BARCODE_COLUMN_DFT = AbstractScreenTrafoModel.GROUP_WELLS_BY_DEFAULT;
	
	public static final String CFG_REGEX = "barcode.pattern"; // no default available

	/**
	 * constructor, adds model settings
	 */
    protected ExpandPlateBarcodeModel() {
    	addModelSetting(ExpandPlateBarcodeModel.CFG_BARCODE_COLUMN, createBarcodeColumnSM());
    	addModelSetting(ExpandPlateBarcodeModel.CFG_REGEX, createBarcodePatternSM());
    }

    /**
     * @return settings model for barcode column
     */
    public static SettingsModelString createBarcodeColumnSM() {
        return new SettingsModelString(CFG_BARCODE_COLUMN, CFG_BARCODE_COLUMN_DFT);
    }
    
    /**
     * @return settings model for barcode pattern
     */
    public static SettingsModelString createBarcodePatternSM() {
    	return new SettingsModelString(CFG_REGEX, null);
    }

    @Override
    protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {

        BufferedDataTable input = inData[0];
        
        String barcodeColumn = ((SettingsModelString) getModelSetting(CFG_BARCODE_COLUMN)).getStringValue();


        TableUpdateCache updateCache = new TableUpdateCache(input.getDataTableSpec());

        Attribute barcodeAttribute = new InputTableAttribute(barcodeColumn, input);


        // 2) create the barcode-parser-factory given the patterns from the preferences
        BarcodeParserFactory bpf = loadFactory();


        // 3) create the additional attributes
        List<String> someBarcodes = collectBarcodes(input, barcodeAttribute);
        Map<String, Attribute> groupMapping = createGroupAttributeMapping(someBarcodes);

        // log which barcode are invalid and plot just one barcode for invalid barcode
        Map<String, Integer> invalidBarcodes = new HashMap<String, Integer>();

        // 4) go through the table again and expand barcodes as possible
        for (DataRow dataRow : input) {
            String barcode = barcodeAttribute.getNominalAttribute(dataRow);

            BarcodeParser barcodeParser = bpf.getParser(barcode);

            // if it was not possible to create a parser update the error log
            if (barcodeParser == null) {
                if (!invalidBarcodes.containsKey(barcode)) {
                    invalidBarcodes.put(barcode, 0);
                }

                // increment the number of how many times the problem occured
                invalidBarcodes.put(barcode, 1 + invalidBarcodes.get(barcode));
            }

            // try to extract all attributes from the given barcode
            for (String groupName : groupMapping.keySet()) {
                Attribute barcodeAttr = groupMapping.get(groupName);

                String barcodeInfo = barcodeParser != null ? barcodeParser.getGroup(groupName) : null;

                if (barcodeInfo == null) {
                    updateCache.add(dataRow, barcodeAttr, DataType.getMissingCell());
                } else {
                    updateCache.add(dataRow, barcodeAttr, barcodeAttr.createCell(barcodeInfo));
                }
            }
        }

        for (String barcode : invalidBarcodes.keySet()) {
            logger.error("The barcode '" + barcode + "' found in " + invalidBarcodes.get(barcode) + " rows is not compatible with the barcode-schemata defined under Preferences->Knime->HCS-Tools");
        }


        // build the output-table
        ColumnRearranger c = updateCache.createColRearranger();
        BufferedDataTable out = exec.createColumnRearrangeTable(input, c, exec);

        return new BufferedDataTable[]{out};
    }


    private List<String> collectBarcodes(BufferedDataTable input, Attribute barcodeAttribute) {
        Set<String> someBarcodes = new HashSet<String>();

        int counter = 0;
        for (DataRow dataRow : input) {
            // just collect a subset of all barchodes
//            if (counter++ > 100) {
//                break;
//            }

            someBarcodes.add(barcodeAttribute.getNominalAttribute(dataRow));
        }

        return new ArrayList<String>(someBarcodes);
    }
    
    /**
     * retrieve a list of barcode patterns from preferences
     * @return
     */
    public static List<String> getPreferencePatterns() {
    	IPreferenceStore prefStore = HCSToolsBundleActivator.getDefault().getPreferenceStore();
        return Arrays.asList(prefStore.getString(HCSToolsPreferenceInitializer.BARCODE_PATTERNS).split(";"));
    }


    public static BarcodeParserFactory loadFactory() {
        IPreferenceStore prefStore = HCSToolsBundleActivator.getDefault().getPreferenceStore();

        List<String> patterns = Arrays.asList(prefStore.getString(HCSToolsPreferenceInitializer.BARCODE_PATTERNS).split(";"));
        return new BarcodeParserFactory(patterns);
    }


    private Map<String, Attribute> createGroupAttributeMapping(List<String> someBarcodes) {
        Set<String> presentGroups = new HashSet<String>();


        BarcodeParserFactory bpf = loadFactory();

        for (String barcode : someBarcodes) {
            BarcodeParser barcodeParser = bpf.getParser(barcode);
            if (barcodeParser != null) {
                presentGroups.addAll(barcodeParser.getAvailableGroups());
            }

        }

        // create attributes for each group
        Map<String, Attribute> groupMapping = new TreeMap<String, Attribute>();
//        for (String presentGroup : presentGroups) {
//            groupMapping.put(presentGroup, new Attribute(bpf.getVerboseName(presentGroup), getAttributeType(presentGroup)));
//        }

        return groupMapping;
    }


    private DataType getColumnType(String groupName) {
        Object groupType = BarcodeParser.groupTypes.get(groupName);

        if (groupType == null) {
            return StringCell.TYPE;
        } else if (groupType.equals(Integer.class)) {
            return IntCell.TYPE;
        } else if (groupType.equals(Double.class)) {
            return DoubleCell.TYPE;
        } else {
            return StringCell.TYPE;
        }
    }


    @Override
    protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
        DataTableSpec tSpec = inSpecs[0];
        
        // get settings if available
        String barcodeColumn = null;
        if(getModelSetting(CFG_BARCODE_COLUMN) != null) barcodeColumn = ((SettingsModelString) getModelSetting(CFG_BARCODE_COLUMN)).getStringValue();
        String barcodePattern = null;
        if(getModelSetting(CFG_REGEX) != null) barcodePattern = ((SettingsModelString) getModelSetting(CFG_REGEX)).getStringValue();
        
        // check if input table has string compatible columns at all
        boolean hasStringColumns = false;
        for(String col : tSpec.getColumnNames()) {
        	if(tSpec.getColumnSpec(col).getType().isCompatible(StringValue.class)) {
        		hasStringColumns = true;
        		break;
        	}
        }
        if(!hasStringColumns) {
        	throw new InvalidSettingsException("Input table must contain at least one string column");
        }
        
        // check whether regex patterns are available at all
        List<String> prefPatterns = getPreferencePatterns();
        if(prefPatterns.isEmpty()) {
        	throw new InvalidSettingsException("No barcode patterns available from Preferences > KNIME > HCS-Tools");
        }
        
        // check if barcode column is available in input column
        if(barcodeColumn != null) {
        	if(!tSpec.containsName(barcodeColumn)) {
        		throw new InvalidSettingsException("Column '" + barcodeColumn + "' is not available in input table. Please reconfigure the node");
        	}
        }
        
        // if barcode pattern is not set, select one pattern from list as a guess
        if(barcodePattern == null) {
        	barcodePattern = prefPatterns.get(0);
        	this.setWarningMessage("No barcode pattern has been set. The first pattern from preferences will be used as default");
        }
        
        // check if a valid pattern has been selected
        NamedPattern pattern = NamedPattern.compile(barcodePattern);
        if(!pattern.isValidPattern()) {
        	throw new InvalidSettingsException("Barcode pattern is not valid. Please check Preferences > KNIME > HCS-Tools");
        }
        
        ColumnRearranger cRearr = createColumnRearranger(tSpec, pattern, tSpec.findColumnIndex(barcodeColumn));

        return new DataTableSpec[]{cRearr.createSpec()};
    }
    
    private ColumnRearranger createColumnRearranger(DataTableSpec inSpec, final NamedPattern pattern, final int bcIdx) {
    	
    	final List<String> groupNames = pattern.groupNames();
    	// create new column specs
    	final int newColCount = groupNames.size();
        final DataColumnSpec[] newColSpecs = new DataColumnSpec[newColCount];
        final LinkedHashMap<String, DataType> typeMapping = new LinkedHashMap<String, DataType>();
        
        for (int i = 0; i < newColCount; i++) {
        	// default data type and group name
        	String group = groupNames.get(i);
        	DataType dtype = StringCell.TYPE;
        	// if it is one of the standard groups, get their defined data type and nice name
        	if(BarcodeParser.longGroupNames.containsKey(group) && BarcodeParser.groupTypes.containsKey(group)) {
        		// first find data type of group, then rename group to a nice name
        		dtype = getColumnType(group);
        		group = BarcodeParser.longGroupNames.get(group);		
        	}
        	typeMapping.put(group, dtype);
            String name = DataTableSpec.getUniqueColumnName(inSpec, group);
            newColSpecs[i] = new DataColumnSpecCreator(
                    name, dtype).createSpec();
        }
    	
    	ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        rearranger.insertAt(bcIdx, new AbstractCellFactory(newColSpecs) {
            /** {@inheritDoc} */
            @Override
            public DataCell[] getCells(final DataRow row) {
                DataCell[] result = new DataCell[newColCount];
                Arrays.fill(result, DataType.getMissingCell());
                DataCell c = row.getCell(bcIdx);
                if (c.isMissing()) {
                    return result;
                }
                
                //get barcode
                String s = ((StringValue)c).getStringValue();
                BarcodeParser parser = new BarcodeParser(s, pattern);
                int i = 0;
                for(String group : typeMapping.keySet()) {
                	DataType dtype = typeMapping.get(group);
                	String substring = parser.getGroup(group);
                	result[i] = de.mpicbg.knime.knutils.Utils.createCellByType(dtype, substring);                		
                }               
                return result;
            }
        });
        return rearranger;
    }
}