package edu.isi.bmkeg.lapdf.xml.model;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

@XmlRootElement(name="word")
public class LapdftextXMLWord
//		extends LapdftextXMLRectangle
		implements Serializable {
	static final long serialVersionUID = 8047039304729208683L;

	private String t;

	private String font;

	private int fId;

	private int sId;

	@XmlValue
	public String getT() {
		return t;
	}

	public void setT(String t) {
		this.t = t;
	}

	@XmlAttribute	
	public int getfId() {
		return fId;
	}

	public void setfId(int fId) {
		this.fId = fId;
	}

	@XmlAttribute	
	public int getsId() {
		return sId;
	}

	public void setsId(int sId) {
		this.sId = sId;
	}

	@XmlAttribute	
	public String getFont() {
		return font;
	}

	public void setFont(String font) {
		this.font = font;
	}

	private int id;
	private int w;
	private int h;
	private int x;
	private int y;
	private int i;

	@XmlAttribute
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@XmlAttribute
	public int getW() {
		return w;
	}

	public void setW(int w) {
		this.w = w;
	}

	@XmlAttribute
	public int getH() {
		return h;
	}

	public void setH(int h) {
		this.h = h;
	}

	@XmlAttribute
	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	@XmlAttribute
	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	@XmlAttribute
	public int getI() {
		return i;
	}

	public void setI(int i) {
		this.i = i;
	}

}
