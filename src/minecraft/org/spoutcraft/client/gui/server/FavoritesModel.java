/*
 * This file is part of Spoutcraft (http://www.spout.org/).
 *
 * Spoutcraft is licensed under the SpoutDev License Version 1.
 *
 * Spoutcraft is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * In addition, 180 days after any changes are published, you can use the
 * software, incorporating those changes, under the terms of the MIT license,
 * as described in the SpoutDev License Version 1.
 *
 * Spoutcraft is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License,
 * the MIT license and the SpoutDev license version 1 along with this program.
 * If not, see <http://www.gnu.org/licenses/> for the GNU Lesser General Public
 * License and see <http://www.spout.org/SpoutDevLicenseV1.txt> for the full license,
 * including the MIT license.
 */
package org.spoutcraft.client.gui.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.yaml.snakeyaml.Yaml;

import org.spoutcraft.client.SpoutClient;
import org.spoutcraft.client.io.FileUtil;
import org.spoutcraft.spoutcraftapi.gui.AbstractListModel;
import org.spoutcraft.spoutcraftapi.gui.ListWidgetItem;

public class FavoritesModel extends AbstractListModel {
	private ArrayList<ServerItem> items = new ArrayList<ServerItem>();
	private GuiFavorites gui;
	private boolean polling = false;

	public FavoritesModel() {
	}

	public synchronized boolean isPolling() {
		return polling;
	}

	public synchronized void setPolling(boolean poll) {
		polling = poll;
		if (gui != null) {
			gui.updateButtons();
		}
	}

	public void setCurrentGUI(GuiFavorites gui) {
		this.gui = gui;
	}

	public ListWidgetItem getItem(int row) {
		return items.get(row);
	}

	public int getSize() {
		return items.size();
	}

	public void load() {
		boolean wasSandboxed = SpoutClient.isSandboxed();
		if (wasSandboxed) SpoutClient.disableSandbox();
		try {
			if (!getFile().exists()) {
				importVanilla();
				importLegacyTXT();
				save();
			} else {
				Yaml yaml = new Yaml();

				try {
					ArrayList<HashMap<String, Object>> list = (ArrayList<HashMap<String, Object>>) yaml.load(new FileReader(getFile()));
					for (HashMap<String, Object> item: list) {
						String title = "";
						String ip = "";
						int port = 25565;
						int databaseId = -1;
						if (item.containsKey("title")) title = (String) item.get("title");
						if (item.containsKey("ip")) ip = (String) item.get("ip");
						if (item.containsKey("port")) port = (Integer) item.get("port");
						if (item.containsKey("databaseid")) databaseId = (Integer) item.get("databaseid");
						addServer(title, ip, port, databaseId);
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (wasSandboxed) SpoutClient.enableSandbox();
		}
	}

	private void importLegacyTXT() {
		boolean wasSandboxed = SpoutClient.isSandboxed();
		if (wasSandboxed) SpoutClient.disableSandbox();
		File favorites = new File(FileUtil.getCacheDirectory(), "favorites.txt");
		if (favorites.exists()) {
			FileReader reader;
			try {
				reader = new FileReader(favorites);

				BufferedReader buffer = new BufferedReader(reader);
				while (true) {
					String line = buffer.readLine();
					if (line == null) break;
					String args[] = line.split(">");
					if (args.length == 2) {
						String title = args[0];
						String split[] = args[1].split(":");
						String ip = split[0];
						int port = split.length > 1 ? Integer.parseInt(split[1]) : 25565;
						addServer(title, ip, port);
					}
					if (args.length == 3) {
						String title = args[1];
						String split[] = args[0].split(":");
						String ip = split[0];
						int port = split.length > 1 ? Integer.parseInt(split[1]) : 25565;
						int databaseId = Integer.parseInt(args[2]);
						addServer(title, ip, port, databaseId);
					}
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			//favorites.delete(); TODO: Check if that is working first...
		}
		if (wasSandboxed) {
			SpoutClient.enableSandbox();
		}
	}

	private void importVanilla() {
		// TODO Auto-generated method stub

	}

	public void save() {
		Yaml yaml = new Yaml();
		ArrayList<Object> list = new ArrayList<Object>();
		for (ServerItem item:items) {
			HashMap<String, Object> data = new HashMap<String, Object>();
			data.put("title", item.getTitle());
			data.put("ip", item.getIp());
			data.put("port", item.getPort());
			data.put("databaseid", item.getDatabaseId());
			list.add(data);
		}
		try {
			boolean wasSandboxed = SpoutClient.isSandboxed();
			if (wasSandboxed) SpoutClient.disableSandbox();
			yaml.dump(list, new FileWriter(getFile()));
			if (wasSandboxed) SpoutClient.enableSandbox();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private File getFile() {
		return new File(FileUtil.getSpoutcraftDirectory(), "favorites.yml");
	}

	public void addServer(String title, String ip, int port) {
		items.add(new ServerItem(title, ip, port, -1));
		sizeChanged();
	}

	public void addServer(String title, String ip, int port, int databaseId) {
		items.add(new ServerItem(title, ip, port, databaseId));
		sizeChanged();
	}

	public void onSelected(int item, boolean doubleClick) {
		gui.updateButtons();
	}

	public void removeServer(ServerItem selectedItem) {
		items.remove(selectedItem);
		sizeChanged();
	}

	public void addServer(ServerItem item) {
		items.add(item);
		sizeChanged();
	}

	public void move(int selectedRow, int i) {
		ServerItem item = items.get(selectedRow);
		i = Math.max(0, i);
		i = Math.min(i, getSize() - 1);
		items.remove(selectedRow);
		items.add(i, item);
	}

	public boolean containsSever(ServerItem item) {
		for (ServerItem obj:items) {
			if (obj.getIp().equals(item.getIp()) && obj.getPort() == item.getPort()) {
				return true;
			}
		}
		return false;
	}

	public GuiFavorites getCurrentGui() {
		return gui;
	}
}
