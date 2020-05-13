package org.telosys.tools.eclipse.plugin.editors.dsl.model;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.telosys.tools.api.GenericModelLoader;
import org.telosys.tools.api.TelosysModelException;
import org.telosys.tools.commons.TelosysToolsException;
import org.telosys.tools.dsl.DslModelManager;
import org.telosys.tools.dsl.DslModelUtil;
import org.telosys.tools.dsl.parser.model.DomainModelInfo;
import org.telosys.tools.eclipse.plugin.commons.MsgBox;
import org.telosys.tools.eclipse.plugin.commons.PluginImages;
import org.telosys.tools.eclipse.plugin.editors.commons.AbstractModelEditor;
import org.telosys.tools.eclipse.plugin.editors.dsl.commons.ModelLoadingResult;
import org.telosys.tools.eclipse.plugin.editors.dsl.commons.ModelManager;
import org.telosys.tools.generic.model.Model;

/**
 * Main entry point for the DSL model editor (for code generation) <br>
 * 
 */
public class ModelEditor extends AbstractModelEditor {
	
	private Image _imageWithoutError = null ;
    private ModelEditorPageModelEntities _modelEntitiesPage    = null ;
	private ModelEditorPageModelInfo     _modelInformationPage = null ;
	
	private List<String>        _entitiesFileNames = null ;
	private Map<String,List<String>>  _entitiesErrors = null ;
	private DomainModelInfo     _modelInfo = null ; 
	
	//----------------------------------------------------------------------------------------
    public ModelEditor() {
    	super();
    }
    
    public List<String> getEntitiesAbsoluteFileNames() {
    	return _entitiesFileNames ;
    }
    
	public Map<String,List<String>> getEntitiesErrors() {
		return _entitiesErrors;
	}
	public DomainModelInfo getDomainModelInfo() {
		return _modelInfo ;
	}
	
    
	//========================================================================================
	// Editor plugin startup ( for each file to edit ) :
	// Step 1 : init()
	// Step 2 : addPages()
	//========================================================================================
	public void closeEditor(boolean save) {
		getSite().getPage().closeEditor(ModelEditor.this, save);
	}
	
	//----------------------------------------------------------------------------------------
    @Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);
		log(this, "init()..." );
		ImageDescriptor imageDescriptor = input.getImageDescriptor();
		if ( imageDescriptor != null ) {
			_imageWithoutError = imageDescriptor.createImage();
		}
	}

    @Override
    public void dispose() {
        super.dispose();
        _imageWithoutError.dispose();
    }
    
	//----------------------------------------------------------------------------------------
    @Override
	protected void addPages() {
		log(this, "addPages()..." );

		_modelEntitiesPage = 
			new ModelEditorPageModelEntities(this, "ModelEditorPage1", " Model entities " );
		
		_modelInformationPage = 
			new ModelEditorPageModelInfo(this, "ModelEditorPage2", " Model information " );
		
		ModelEditorPageCodeGeneration codeGenerationPage = 
			new ModelEditorPageCodeGeneration(this, "ModelEditorPage3", " Code generation " );
		
		try {
			addPage(_modelEntitiesPage);
			addPage(_modelInformationPage);
			addPage(codeGenerationPage);
		} catch (PartInitException e) {
			MsgBox.error("RepositoryEditor : addPage(page) throws PartInitException ", e);
		}		
		setCodeGenerationPage(codeGenerationPage);
		
		log(this, "addPages() : DONE" );
	}

    //----------------------------------------------------------------------------------------
/***
    @Override // implements super class abstract method
    protected Model loadModel_OLD(File modelFile) {
		//log("loadModel(" + modelFile + ")");
    	//--- 1) Load entities absolute file names 
    	_entitiesFileNames = DslModelUtil.getEntitiesAbsoluteFileNames(modelFile);
    	
    	//--- 2) Load model information from ".model" file
		DslModelManager modelManager = new DslModelManager();
		_modelInfo = modelManager.loadModelInformation(modelFile);

    	//--- 3) Prepare the model loader and try to load the model
		GenericModelLoader genericModelLoader = new GenericModelLoader( getProjectConfig() ) ;
		
		Model model;
		try {
			//--- 3.1) Try to load/parse the model
			model = genericModelLoader.loadModel(modelFile);
			//--- 3.2) Model OK : no parsing error
			_entitiesErrors = null ;
			return model;
		} catch (TelosysModelException modelException) {
			//--- 3.2) Invalid Model : keep parsing errors
//			// #TMP
//			MsgBox.debug("Invalid model !\n TelosysModelException : " + 
//					" \n message : " + modelException.getMessage() + 
//					" \n errors size : " + modelException.getParsingErrors().size() ) ;
			_entitiesErrors = modelException.getParsingErrors();
			return null ;
		} catch (TelosysToolsException e) {
			MsgBox.error("Cannot load model !\n Unexpected exception" , e );
			return null ;
		}
    }
***/
    @Override // implements super class abstract method
    protected Model loadModel(File modelFile) { // called by super.loadModel()
    	// Try to load the DSL model
    	ModelManager modelManager = new ModelManager();
    	ModelLoadingResult r = modelManager.load(modelFile);
    	// Update editor views with the result (model OK or errors)
    	updateEditor(r);
    	return r.getModel(); 
    }
    
    public void updateEditor(ModelLoadingResult r) {

    	// Set editor title image : errors or not
		if ( r.getEntitiesErrors() != null && ! r.getEntitiesErrors().isEmpty() ) {
			Image errorImage = PluginImages.getImage(PluginImages.ERROR);
			log(this, "refresh() : setTitleImage(errorImage) " );
			setTitleImage(errorImage);
		} else {
			setTitleImage(_imageWithoutError);
		}

		// Refresh entities list in first page
		_modelEntitiesPage.populateEntities(r.getEntitiesFileNames(), r.getEntitiesErrors()); 
    }
    
    //----------------------------------------------------------------------------------------
    @Override // implements super class abstract method
    protected void saveModel( Model model, File modelFile ) {
    	if ( _modelInformationPage != null ) {
        	if  ( _modelInfo != null ) {
            	_modelInformationPage.updateModelInformation(_modelInfo);
        		DslModelManager modelManager = new DslModelManager();
        		modelManager.saveModelInformation(modelFile, _modelInfo);
        	}
        	else {
        		MsgBox.error("ModelInfo is null in the editor !");
        	}
    	}
    	else {
    		MsgBox.error("_modelInformationPage is null in the editor !");
    	}
    }
    //----------------------------------------------------------------------------------------
    /**
     * Refresh the model status in the model editor <br>
     * - reload the model <br>
     * - populate the entities lists (with errors messages if any ) <br>
     * - update the editor image according with the current status (errors or not) <br>
     * 
     */
    public void refresh() {
		log(this, "refresh()..." );

		this.loadModel(); // call super.loadModel() --> call this.loadModel(File) 
    	
//    	// Set title image : errors or not
//		if ( r.getEntitiesErrors() != null && ! r.getEntitiesErrors().isEmpty() ) {
//			Image errorImage = PluginImages.getImage(PluginImages.ERROR);
//			log(this, "refresh() : setTitleImage(errorImage) " );
//			setTitleImage(errorImage);
//		} else {
//			setTitleImage(_imageWithoutError);
//		}
//		_modelEntitiesPage.populateEntities(r.getEntitiesFileNames(), r.getEntitiesErrors()); 
		
//		int errorsCount = _modelEntitiesPage.populateEntities();    
//		log(this, "refresh() : errorsCount = " + errorsCount);
//		if ( errorsCount > 0 ) {
//			Image errorImage = PluginImages.getImage(PluginImages.ERROR);
//			log(this, "refresh() : setTitleImage(errorImage) " );
//			setTitleImage(errorImage);
//		}
//		else {
//			setTitleImage(_imageWithoutError);
//		}
    }
}