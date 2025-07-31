package ca.bkaw.torque.assets.model;

import com.google.gson.JsonArray;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Manages multiple element extractions from a model while handling index conflicts.
 * <p>
 * This class ensures that when multiple groups of elements need to be extracted,
 * the operation is done safely without index conflicts.
 */
public class ModelExtractor {
    private final Model model;
    private final List<ExtractionRequest> requests = new ArrayList<>();
    private boolean executed = false;

    /**
     * An object that keeps track of an extraction request.
     *
     * @param name The name of the extracted model.
     * @param elementIndexes The indexes of elements to extract.
     */
    private record ExtractionRequest(String name, IntList elementIndexes) {}

    /**
     * An element that is to be extracted.
     *
     * @param modelName The name of the extracted model.
     * @param element The element to extract.
     * @param originalIndex The original index of the element in the model.
     */
    private record ElementMapping(String modelName, ModelElement element, int originalIndex) {}

    public ModelExtractor(@NotNull Model model) {
        this.model = model;
    }
    
    /**
     * Add an extraction request for a group of elements.
     * 
     * @param name A name to identify this extraction
     * @param elementIndexes The indexes of elements to extract
     */
    public void addExtraction(@NotNull String name, @NotNull IntList elementIndexes) {
        if (this.executed) {
            throw new IllegalStateException("Cannot add extractions after execution.");
        }
        this.requests.add(new ExtractionRequest(name, new IntArrayList(elementIndexes)));
    }
    
    /**
     * Execute all pending extractions and return a map of extracted models.
     * This method ensures that all extractions are performed correctly despite
     * changing indexes.
     * 
     * @return A map from extraction name to the extracted model
     */
    @NotNull
    public Map<String, Model> executeExtractions() {
        this.executed = true;
        Map<String, Model> results = new HashMap<>();
        
        if (this.requests.isEmpty()) {
            return results;
        }
        
        ModelElementList allElements = this.model.getAllElements();
        if (allElements == null) {
            return results;
        }
        
        // Create a list to track which elements belong to which extraction
        List<ElementMapping> elementMappings = new ArrayList<>();
        
        // Map all element indexes to their extraction requests
        for (ExtractionRequest request : this.requests) {
            for (int i = 0; i < request.elementIndexes.size(); i++) {
                int elementIndex = request.elementIndexes.getInt(i);
                if (elementIndex >= 0 && elementIndex < allElements.getElements().size()) {
                    ModelElement element = allElements.getElement(elementIndex);
                    elementMappings.add(new ElementMapping(request.name, element, elementIndex));
                }
            }
        }
        
        // Sort by index in descending order for safe removal
        elementMappings.sort((a, b) -> Integer.compare(b.originalIndex, a.originalIndex));
        
        // Group by extraction model name
        Map<String, List<ModelElement>> extractedElementsByName = new HashMap<>();
        for (ExtractionRequest request : this.requests) {
            extractedElementsByName.put(request.name, new ArrayList<>());
        }
        
        // Remove elements from the original model and collect them by extraction model name
        for (ElementMapping mapping : elementMappings) {
            allElements.remove(mapping.originalIndex);
            // add first to maintain order
            extractedElementsByName.get(mapping.modelName).addFirst(mapping.element);
        }
        
        // Create new models for each extraction
        for (ExtractionRequest request : this.requests) {
            Model extractedModel = new Model();
            List<ModelElement> extractedElements = extractedElementsByName.get(request.name);
            
            if (!extractedElements.isEmpty()) {
                ModelElementList newElements = new ModelElementList(new JsonArray());
                for (ModelElement element : extractedElements) {
                    newElements.add(element);
                }
                extractedModel.setElements(newElements);
            }
            
            results.put(request.name, extractedModel);
        }

        // Remove groups since the indexes of elements have changed,
        // and we don't want to keep any stale references.
        this.model.removeGroups();
        
        return results;
    }

}
