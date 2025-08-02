package ca.bkaw.torque.tags;

import ca.bkaw.torque.assets.model.Model;
import ca.bkaw.torque.assets.model.ModelElement;
import ca.bkaw.torque.assets.model.ModelExtractor;
import ca.bkaw.torque.model.TagHandler;
import ca.bkaw.torque.model.TagString;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A tag handler that processes elements tagged with #seat to create seat objects.
 * <p>
 * This is an element-based handler, meaning each tagged element creates a separate seat.
 */
public class SeatTags implements TagHandler<List<SeatTags.Seat>> {
    public static final double VERTICAL_OFFSET = 0.7; // unit: blocks (meters)

    /**
     * @param translation The translation from the vehicle's center of mass to the position that the
     *                    display entity should be at to position a player as a passenger in this seat.
     *                    Measured local coordinates relative to the vehicle.
     * @param driver Whether this seat is the driver's seat.
     */
    public record Seat(Vector3fc translation, boolean driver) {}

    @Override
    public List<Seat> process(@NotNull Model model, @NotNull ModelExtractor modelExtractor) {
        List<Seat> seats = new ArrayList<>();
        List<ModelElement> seatElements = model.getElementsByTag("seat");
        
        for (ModelElement element : seatElements) {
            processElement(element, seats);
        }
        
        return seats;
    }
    
    /**
     * Process a single seat element and add it to the seat list.
     */
    private void processElement(@NotNull ModelElement element, @NotNull List<Seat> seats) {
        TagString tags = element.getTags();
        boolean isDriver = tags.hasTag("driver");
        
        // Use the middle as the seat position, however vertically we want to use the lowest point.
        Vector3d seatPosition = element.getMiddle();
        seatPosition.y = Math.min(element.getFrom().y, element.getTo().y);
        seatPosition.div(16); // Convert from model units ("pixels") to blocks.
        seatPosition.add(0, VERTICAL_OFFSET, 0);

        seats.add(new Seat(
            new Vector3f(seatPosition),
            isDriver
        ));
    }

}
