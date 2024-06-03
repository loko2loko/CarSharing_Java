package carsharing;

@FunctionalInterface
public interface Command {

    Command exec();
}