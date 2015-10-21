package de.mpicbg.knime.hcs.base.nodes.layout.createwellposition;

import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;





/**
 * <code>NodeDialog</code> for the "CreateWellPosition" Node.
 * 
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author 
 */
public class CreateWellPositionNodeDialog extends DefaultNodeSettingsPane {


    @SuppressWarnings("unchecked")
    public CreateWellPositionNodeDialog() {
	super();
	
	// adding Dialog for selecting the column
	addDialogComponent(new DialogComponentColumnNameSelection(CreateWellPositionNodeModel.createPlateColumn(),
			"Select the Plate Column",
			0,
			DoubleValue.class, StringValue.class));     

	// adding Dialog for selecting the row
	addDialogComponent(new DialogComponentColumnNameSelection(CreateWellPositionNodeModel.createPlateRow(), 
			"Select the Plate Row",
			0,
			DoubleValue.class, StringValue.class)); 
	
	// adding Dialog for optional deleting the source column
	addDialogComponent(new DialogComponentBoolean(CreateWellPositionNodeModel.createDelSourceCol(), "Delete the Source Columns"));
	
	// adding Dialog for optional changing format into an sortable
	addDialogComponent(new DialogComponentBoolean(CreateWellPositionNodeModel.createFormateColumn(), "Convert in sorting formate"));
    }



}

