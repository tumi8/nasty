package de.japes.net.nasty.collector;

public class Identifier {
	
	private long id;
	
	public Identifier() {};
	
	public Identifier (long id) {
		
		this.id = id;
	}
	
	public long getID() {
		return id;
	}
	public void setID(long id) {
		this.id = id;
	}
	public int hashCode() {
		return (int)id;
	}
	public boolean equals(Object o) {
		
		return (o instanceof Identifier) &&
			this.id == ((Identifier)o).id;
	}
}
