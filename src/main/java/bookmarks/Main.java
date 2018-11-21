package bookmarks;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import bookmarks.dao.*;
import bookmarks.domain.Entry;
import bookmarks.domain.Tag;
import bookmarks.io.ConsoleIO;
import bookmarks.io.IO;

public class Main {
	private EntryDao entryDao;
	private IO io;

	public Main(IO io) {
		this.io = io;
		Database database = new Database("jdbc:sqlite:bookmarks.db");
		database.getConnection();
		database.createNewTables();

		TagDao tagDao = new TagDao(database);
		EntryTagDao entryTagDao = new EntryTagDao(database, tagDao);
		EntryMetadataDao entryMetadataDao = new EntryMetadataDao(database);
		entryDao = new EntryDao(database, entryTagDao, entryMetadataDao);
	}

	public void addCommand() {
		Map<String, String> metadata = new HashMap<>();
		metadata.put("type", io.readLine("Type: "));
		metadata.put("title", io.readLine("Title: "));
		metadata.put("author", io.readLine("Author: "));
		metadata.put("description", io.readLine("Description: "));
		metadata.put("comment", io.readLine("Comment: "));
		Set<Tag> tags = Arrays.stream(
			io.readLine("Tags: ")
				.split(","))
			.map(s -> new Tag("tag", s.trim()))
			.collect(Collectors.toSet());

		try {
			entryDao.save(new Entry(tags, metadata));
			io.print("Entry created");
		} catch (Exception e) {
			io.print("Failed to save :(");
			e.printStackTrace();
		}
	}

	public void editCommand() {
		int id = io.readInt("ID to edit: ");
		Entry entry;
		try {
			entry = entryDao.findOne(id);
		} catch (SQLException e) {
			io.print("Failed to fetch entry :(");
			e.printStackTrace();
			return;
		}

		if (entry == null) {
			io.print("Entry not found");
			return;
		}

		Map<String, String> metadata = entry.getMetadata();
		for (Map.Entry<String, String> meta : metadata.entrySet()) {
			String key = meta.getKey();
			key = key.substring(0, 1).toUpperCase() + key.substring(1);
			String newVal = io.readLine(String.format("%s [%s]: ", key, meta.getValue()));
			if (!newVal.isEmpty()) {
				metadata.put(meta.getKey(), newVal);
			}
		}
		String newTags = io.readLine(String.format("Tags [%s]: ", entry
			.getTags().stream()
			.map(Tag::getName)
			.collect(Collectors.joining(", "))));
		if (!newTags.isEmpty()) {
			entry.setTags(Arrays.stream(
				newTags.split(","))
				.map(s -> new Tag("tag", s.trim()))
				.collect(Collectors.toSet()));
		}

		try {
			entryDao.save(entry);
			io.print("Entry updated");
		} catch (SQLException e) {
			io.print("Failed to save entry :(");
			e.printStackTrace();
		}
	}

	public void listCommand() {
		try {
			List<Entry> entries = entryDao.findAll();
			if (entries.isEmpty()) {
				io.print("No entries :(");
			}
			for (Entry entry : entries) {
				io.print(entry.toString());
			}
		} catch (Exception e) {
			io.print("Failed to get list :(");
			e.printStackTrace();
		}
	}

	public void helpCommand() {
		io.print("add  - add a new entry");
		io.print("edit - edit an existing entry");
		io.print("list - list all entries");
		io.print("quit - exits the program");
		io.print("help - print this screen");
	}

	public void run() {
		io.print("bookmarks v0.1.0");
		helpCommand();
		while (true) {
			String comm = io.readLine("> ");
			switch (comm) {
				case "":
					break;
				case "add":
					addCommand();
					break;
				case "edit":
					editCommand();
					break;
				case "list":
					listCommand();
					break;
				case "quit":
					return;
				case "help":
					helpCommand();
					break;
				default:
					io.print("unrecognized command");
			}
		}
	}

	public static void main(String[] args) {
		IO io = new ConsoleIO();
		new Main(io).run();
	}
}
