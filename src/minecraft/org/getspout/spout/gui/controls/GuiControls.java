package org.getspout.spout.gui.controls;

import net.minecraft.src.GuiScreen;

import org.getspout.spout.client.SpoutClient;
import org.getspout.spout.controls.Shortcut;
import org.getspout.spout.controls.SimpleKeyBindingManager;
import org.getspout.spout.gui.ButtonUpdater;
import org.getspout.spout.gui.GuiSpoutScreen;
import org.getspout.spout.gui.controls.ControlsModel.ControlsBasicItem;
import org.getspout.spout.gui.controls.ControlsModel.KeyBindingItem;
import org.getspout.spout.gui.controls.ControlsModel.ShortcutBindingItem;
import org.getspout.spout.gui.shortcuts.GuiEditShortcut;
import org.spoutcraft.spoutcraftapi.ChatColor;
import org.spoutcraft.spoutcraftapi.Spoutcraft;
import org.spoutcraft.spoutcraftapi.addon.Addon;
import org.spoutcraft.spoutcraftapi.gui.Button;
import org.spoutcraft.spoutcraftapi.gui.CheckBox;
import org.spoutcraft.spoutcraftapi.gui.GenericButton;
import org.spoutcraft.spoutcraftapi.gui.GenericCheckBox;
import org.spoutcraft.spoutcraftapi.gui.GenericLabel;
import org.spoutcraft.spoutcraftapi.gui.GenericListView;
import org.spoutcraft.spoutcraftapi.gui.GenericScrollArea;
import org.spoutcraft.spoutcraftapi.gui.Keyboard;
import org.spoutcraft.spoutcraftapi.gui.Label;
import org.spoutcraft.spoutcraftapi.gui.Orientation;
import org.spoutcraft.spoutcraftapi.gui.ScrollArea;
import org.spoutcraft.spoutcraftapi.gui.Widget;

public class GuiControls extends GuiSpoutScreen implements ButtonUpdater{
	private Label labelTitle, labelDescription;
	private Button buttonDone, buttonAdd, buttonEdit, buttonRemove;
	public CheckBox checkVanilla, checkShortcuts, checkBindings;
	public ControlsSearch search;
	private ScrollArea filter;
	private GenericListView view;
	private GuiScreen parentScreen;
	private static ControlsModel model = null;
	
	public static final ChatColor VANILLA_COLOR = ChatColor.YELLOW;
	public static final ChatColor SHORTCUTS_COLOR = ChatColor.GREEN;
	public static final ChatColor BINDINGS_COLOR = ChatColor.BLUE;

	public GuiControls(GuiScreen parent) {
		if(model == null) {
			model = new ControlsModel(this);
		}
		this.parentScreen = parent;
	}

	protected void createInstances() {
		Addon spoutcraft = Spoutcraft.getAddonManager().getAddon("Spoutcraft");
		labelTitle = new GenericLabel("Controls");
		buttonDone = new GenericButton("Done");
		buttonAdd = new GenericButton("Add Shortcut");
		buttonEdit = new GenericButton("Edit");
		buttonEdit.setTooltip("Edit Shortcut");
		buttonRemove = new GenericButton("Remove");
		buttonRemove.setTooltip("Remove Shortcut");
		labelDescription = new GenericLabel();
		labelDescription.setText("Double-click an item, then press the key (combination).");
		filter = new GenericScrollArea();
		view = new GenericListView(model);
		model.setCurrentGui(this);
		
		checkVanilla = new ControlsCheckBox(this, VANILLA_COLOR+"Vanilla Bindings");
		checkShortcuts = new ControlsCheckBox(this, SHORTCUTS_COLOR+"Shortcuts");
		checkBindings = new ControlsCheckBox(this, BINDINGS_COLOR+"Bindings");
		search = new ControlsSearch(this);
		
		filter.attachWidget(spoutcraft, checkVanilla);
		filter.attachWidget(spoutcraft, checkShortcuts);
		filter.attachWidget(spoutcraft, checkBindings);
		
		getScreen().attachWidget(spoutcraft, search);
		getScreen().attachWidget(spoutcraft, labelTitle);
		getScreen().attachWidget(spoutcraft, filter);
		getScreen().attachWidget(spoutcraft, view);
		getScreen().attachWidget(spoutcraft, buttonAdd);
		getScreen().attachWidget(spoutcraft, buttonEdit);
		getScreen().attachWidget(spoutcraft, buttonRemove);
		getScreen().attachWidget(spoutcraft, buttonDone);
		getScreen().attachWidget(spoutcraft, labelDescription);
		updateButtons();
		model.refresh();
	}

	protected void layoutWidgets() {
		int top = 5;

		int swidth = mc.fontRenderer.getStringWidth(labelTitle.getText());
		labelTitle.setY(top + 7).setX(width / 2 - swidth / 2).setHeight(11).setWidth(swidth);
		
		search.setX(5).setY(top).setWidth(150).setHeight(20);

		top+=25;

		int sheight = height - top - 55;

		filter.setX(5).setY(top).setWidth(130).setHeight(sheight);

		view.setX((int) (5 + filter.getX() + filter.getWidth())).setY(top).setWidth((int) (width - 15 - filter.getWidth())).setHeight(sheight);
		
		int ftop = 5;
		checkVanilla.setX(5).setY(ftop).setWidth(100).setHeight(20);
		ftop += 25;
		checkShortcuts.setX(5).setY(ftop).setWidth(100).setHeight(20);
		ftop += 25;
		checkBindings.setX(5).setY(ftop).setWidth(100).setHeight(20);
		
		for(Widget w:filter.getAttachedWidgets()) {
			w.setWidth(filter.getViewportSize(Orientation.HORIZONTAL) - 10);
		}
		search.setWidth((int) filter.getWidth());

		top += 5 + view.getHeight();

		int totalWidth = Math.min(width - 10, 200*3+10);
		int cellWidth = (totalWidth - 10) / 3;
		int left = width / 2 - totalWidth / 2;
		int center = left + 5 + cellWidth;
		int right = center + 5 + cellWidth;

		buttonAdd.setHeight(20).setWidth(cellWidth).setX(left).setY(top);

		buttonEdit.setHeight(20).setWidth(cellWidth).setX(center).setY(top);

		buttonRemove.setHeight(20).setWidth(cellWidth).setX(right).setY(top);

		top+=25;

		labelDescription.setHeight(20).setWidth(cellWidth * 2 + 5).setX(left).setY(top);

		buttonDone.setHeight(20).setWidth(cellWidth).setX(right).setY(top);
	}

	@Override
	protected void buttonClicked(Button btn) {
		SimpleKeyBindingManager man = (SimpleKeyBindingManager) SpoutClient.getInstance().getKeyBindingManager();
		if(btn.equals(buttonDone)) {
			mc.displayGuiScreen(parentScreen);
			return;
		}

		if(btn.equals(buttonAdd)) {
			Shortcut sh = new Shortcut();
			sh.setTitle("");
			sh.setKey(Keyboard.KEY_UNKNOWN.getKeyCode());
			editItem(sh);
			return;
		}
		ControlsBasicItem item = model.getItem(view.getSelectedRow());
		ShortcutBindingItem sh = null;
		if(item != null && item instanceof ShortcutBindingItem) {
			sh = (ShortcutBindingItem) item;
		}
		KeyBindingItem binding = null;
		if(item != null && item instanceof KeyBindingItem) {
			binding = (KeyBindingItem) item;
		}
		if(sh != null && btn.equals(buttonEdit)) {
			editItem(sh.getShortcut());
		} else if(btn.equals(buttonEdit) && item != null) {
			model.setEditing(item);
		} else if(sh != null && btn.equals(buttonRemove)) {
			man.unregisterShortcut(sh.getShortcut());
			man.save();
			model.refresh();
		} else if(binding != null && btn.equals(buttonRemove)) {
			man.unregisterControl(binding.getBinding());
			man.save();
			model.refresh();
		}
	}
	
	public void editItem(Shortcut item) {
		GuiEditShortcut gui = new GuiEditShortcut(this, item);
		mc.displayGuiScreen(gui);
	}

	public void updateButtons() {
		ControlsBasicItem item = model.getItem(view.getSelectedRow());
		buttonEdit.setEnabled(item != null);
		buttonRemove.setEnabled(item instanceof ShortcutBindingItem || item instanceof KeyBindingItem);
	}

	@Override
	protected void keyTyped(char c, int i) {
		ControlsBasicItem item = model.getEditingItem();
		if(item != null) {
			if(item.useModifiers() && !SimpleKeyBindingManager.isModifierKey(i)) {
				item.setModifiers(SimpleKeyBindingManager.getPressedModifiers());
				item.setKey(i);
				model.finishEdit();
			} else if(!item.useModifiers()){
				item.setKey(i);
				model.finishEdit();
			}
		}
	}

	@Override
	protected void mouseClicked(int x, int y, int button) {
		ControlsBasicItem item = model.getEditingItem();
		if(item != null && item.useMouseButtons()) {
			item.setKey(ControlsModel.MOUSE_OFFSET + button);
			model.finishEdit();
		}
	}

	public ControlsModel getModel() {
		return model;
	}

}
