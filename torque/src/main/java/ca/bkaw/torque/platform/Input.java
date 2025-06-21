package ca.bkaw.torque.platform;

public class Input {
    public boolean forward;
    public boolean backward;
    public boolean left;
    public boolean right;
    public boolean jump;
    public boolean shift;
    public boolean sprint;

    public void reset() {
        this.forward = false;
        this.backward = false;
        this.left = false;
        this.right = false;
        this.jump = false;
        this.shift = false;
        this.sprint = false;
    }

    public boolean notEmpty() {
        return this.forward || this.backward || this.left || this.right || this.jump || this.shift || this.sprint;
    }

    public void merge(Input other) {
        this.forward = this.forward | other.forward;
        this.backward = this.backward | other.backward;
        this.left = this.left | other.left;
        this.right = this.right | other.right;
        this.jump = this.jump | other.jump;
        this.shift = this.shift | other.shift;
        this.sprint = this.sprint | other.sprint;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("Input{");
        if (this.forward) str.append("forward, ");
        if (this.backward) str.append("backward, ");
        if (this.left) str.append("left, ");
        if (this.right) str.append("right, ");
        if (this.jump) str.append("jump, ");
        if (this.shift) str.append("shift, ");
        if (this.sprint) str.append("sprint, ");
        if (str.length() > 6) { // Remove last comma and space
            str.setLength(str.length() - 2);
        }
        str.append('}');
        return str.toString();
    }

}
