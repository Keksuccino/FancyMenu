package de.keksuccino.resources.resourcepack;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import net.minecraft.client.resources.FolderResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.client.resources.data.PackMetadataSection;
import net.minecraft.util.text.TextComponentString;

public class CustomFolderResourcePack extends FolderResourcePack {

	private String name;
	
	public CustomFolderResourcePack(File resourcePackDir, String name) {
		super(resourcePackDir);
		this.name = name;
	}
	
	@Override
	public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer metadataSerializer, String metadataSectionName) throws IOException {
		return (T) new PackMetadataSection(new TextComponentString(this.name), 3);
	}
	
	@Override
	public BufferedImage getPackImage() throws IOException {
		BufferedImage i = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		i.setRGB(0, 0, Color.WHITE.getRGB());
		return i;
	}
	
	@Override
	public String getPackName() {
		return this.name;
	}
	
	@Override
	protected boolean hasResourceName(String name) {
		return (this.name != null);
	}
	
	@Override
	protected void logNameNotLowercase(String name) {}
}
