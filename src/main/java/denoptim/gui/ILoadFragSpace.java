package denoptim.gui;

/**
 * Implemented by GUI components that want to allow user-driven loading of the 
 * fragment space.
 */
public interface ILoadFragSpace
{

    public void renderForPresenceOfFragSpace();

    public void renderForLackOfFragSpace();

}
