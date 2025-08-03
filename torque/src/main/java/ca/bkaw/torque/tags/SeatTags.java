package ca.bkaw.torque.tags;

import ca.bkaw.torque.assets.TorqueAssets;
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

/**
 * A tag handler that processes elements tagged with #seat to create seat objects.
 * <p>
 * This is an element-based handler, meaning each tagged element creates a separate seat.
 */
public class SeatTags implements TagHandler<List<SeatTags.Seat>> {
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
        Vector3d seatElementPosition = element.getMiddle();
        seatElementPosition.y = Math.min(element.getFrom().y, element.getTo().y);
        Vector3d seatOffset = TorqueAssets.getElementOffset((seatElementPosition));

        seats.add(new Seat(
            new Vector3f(seatOffset),
            isDriver
        ));
    }

}
