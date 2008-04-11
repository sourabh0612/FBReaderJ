/*
 * Copyright (C) 2007-2008 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */
package org.geometerplus.fbreader.collection;

import java.io.*;
import java.util.*;
import org.geometerplus.zlibrary.core.util.*;

import org.geometerplus.fbreader.description.Author;
import org.geometerplus.fbreader.description.BookDescription;
import org.geometerplus.fbreader.description.BookDescriptionUtil;
import org.geometerplus.fbreader.formats.FormatPlugin.PluginCollection;
import org.geometerplus.zlibrary.core.filesystem.ZLDir;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.options.ZLBooleanOption;
import org.geometerplus.zlibrary.core.options.ZLIntegerRangeOption;
import org.geometerplus.zlibrary.core.options.ZLOption;
import org.geometerplus.zlibrary.core.options.ZLStringOption;

public class BookCollection {
	public final ZLStringOption PathOption;
	public final ZLBooleanOption ScanSubdirsOption;
	private final static String OPTIONS = "Options";

	private final ArrayList/*Author*/ myAuthors = new ArrayList();
	private	HashMap/*<Author, ArrayList<Description>>*/ myCollection = new HashMap();
	private	final ArrayList myExternalBooks = new ArrayList();

	private	String myPath;
	private	boolean myScanSubdirs;
	private	boolean myDoStrongRebuild;
	private	boolean myDoWeakRebuild;

	public BookCollection() {
		PathOption = new ZLStringOption(ZLOption.CONFIG_CATEGORY, OPTIONS, "BookPath", "/home/geometer/FBooks");
		ScanSubdirsOption = new ZLBooleanOption(ZLOption.CONFIG_CATEGORY, OPTIONS, "ScanSubdirs", false);
		myDoStrongRebuild = true;
		myDoWeakRebuild = false;
	}
	
	private void addExternalBook(BookDescription bookDescription) {
		if (!myExternalBooks.contains(bookDescription)) {
			myExternalBooks.add(bookDescription);
		}
	}

	public ArrayList authors() {
		synchronize();
		return myAuthors;
	}
	
	public ArrayList books(Author author) {
		synchronize();
		return (ArrayList)myCollection.get(author);
	}
	
	public boolean isBookExternal(BookDescription description) {
		synchronize();
		return myExternalBooks.contains(description);
	}

	public	void rebuild(boolean strong) {
		if (strong) {
			myDoStrongRebuild = true;
		} else {
			myDoWeakRebuild = true;
		}
	}
	
	public	boolean synchronize() {
		boolean doStrongRebuild =
			myDoStrongRebuild ||
			(myScanSubdirs != ScanSubdirsOption.getValue()) ||
			(myPath != PathOption.getValue());

		if (!doStrongRebuild && !myDoWeakRebuild) {
			return false;
		}

		myPath = PathOption.getValue();
		myScanSubdirs = ScanSubdirsOption.getValue();
		myDoWeakRebuild = false;
		myDoStrongRebuild = false;
		
		if (doStrongRebuild) {
			myAuthors.clear();
			myCollection.clear();
			myExternalBooks.clear();

			final ArrayList fileNamesSet = collectBookFileNames();
			final int sizeOfSet = fileNamesSet.size();
			for (int i = 0; i < sizeOfSet; ++i) {
				addDescription(BookDescription.getDescription((String)fileNamesSet.get(i)));
			}
			
			final ArrayList bookListFileNames = new BookList().fileNames();
			final int sizeOfList = bookListFileNames.size();
			for (int i = 0; i < sizeOfList; ++i) {
				final String fileName = (String)bookListFileNames.get(i);
				if (!fileNamesSet.contains(fileName)) {
					BookDescription description = BookDescription.getDescription(fileName);
					if (description != null) {
						addDescription(description);
						addExternalBook(description);
					}
				}
			}
		} else {
			//System.out.println("strange code form BookCollection");
			// something strange :(
			final BookList bookList = new BookList();
			final ArrayList bookListSet = bookList.fileNames();
			final ArrayList fileNames = new ArrayList();

//		TEMPORARY COMMENTED FOR J2ME COMPILABILITY
			final ArrayList list = new ArrayList(myCollection.keySet());
			for (int i = 0; i < list.size(); ++i) {
				final ArrayList books = (ArrayList)myCollection.get(list.get(i));
				final int numberOfBooks = books.size();
				for (int j = 0; j < numberOfBooks; ++j) {
					final BookDescription description = (BookDescription)books.get(j);
					final String fileName = description.getFileName();
					if (!myExternalBooks.contains(description) || bookListSet.contains(fileName)) {
						fileNames.add(fileName);
					}
				}
			}

			myCollection.clear();
			myAuthors.clear();
			final int fileNamesSize = fileNames.size();
			for (int i = 0; i < fileNamesSize; ++i) {
				addDescription(BookDescription.getDescription((String)fileNames.get(i), false));
			}		
		}
		
		if (myAuthors != null) {
			Collections.sort(myAuthors, new Author.AuthorComparator());
		}
		DescriptionComparator descriptionComparator = new DescriptionComparator();
		for (Iterator it = myCollection.keySet().iterator(); it.hasNext();) {
			ArrayList list = (ArrayList)myCollection.get(it.next());
			if (list != null) {
				Collections.sort(list, descriptionComparator);
			}
		}
		return true;
	}
		
	private ArrayList collectDirNames() {
		ArrayList nameQueue = new ArrayList();
		ArrayList nameSet = new ArrayList();
		
		String path = myPath;
		int pos = path.indexOf(File.pathSeparator);
		while (pos != -1) {
			nameQueue.add(path.substring(0, pos));
			path = path.substring(0, pos + 1);
			pos = path.indexOf(File.pathSeparator);
		}
		if (path.length() != 0) {
			nameQueue.add(path);
		}

		while (!nameQueue.isEmpty()) {
			String name = (String)nameQueue.get(0);
			nameQueue.remove(0);
			if (!nameSet.contains(name)) {
				if (myScanSubdirs) {
					ZLDir dir = new ZLFile(name).getDirectory();
					if (dir != null) {
						ArrayList subdirs = dir.collectSubDirs();
						for (int i = 0; i < subdirs.size(); ++i) {
							nameQueue.add(dir.getItemPath((String)subdirs.get(i)));
						}
					}
				}
				nameSet.add(name);
			}
		}
		return nameSet;
	}
	
	private ArrayList collectBookFileNames() {
		final ArrayList bookFileNames = new ArrayList();
		final ArrayList dirs = collectDirNames();
		final int numberOfDirs = dirs.size();
		for (int i = 0; i < numberOfDirs; ++i) {
			final String dirfile = (String)dirs.get(i);
			final ZLDir dir = new ZLFile(dirfile).getDirectory();
			if (dir == null) {
				continue;
			}

			final ArrayList files = dir.collectFiles();

			final int numberOfFiles = files.size();
			for (int j = 0; j < numberOfFiles; ++j) {
				String fileName = dir.getItemPath((String)files.get(j));
				ZLFile file = new ZLFile(fileName);
				if (PluginCollection.instance().getPlugin(file, true) != null) {
					if (!bookFileNames.contains(fileName)) {
						bookFileNames.add(fileName);
					}
				// TODO: zip -> any archive
				} else if (file.getExtension().equals("zip")) {
					if (!BookDescriptionUtil.checkInfo(file)) {
						BookDescriptionUtil.resetZipInfo(file);
						BookDescriptionUtil.saveInfo(file);
					}
					final ArrayList zipEntries = new ArrayList();
					BookDescriptionUtil.listZipEntries(file, zipEntries);
					final int numberOfZipEntries = zipEntries.size();
					for (int k = 0; k < numberOfZipEntries; ++k) {
						String str = (String)zipEntries.get(k);
						if (!bookFileNames.contains(str)) {
							bookFileNames.add(str);
						}
					}
				}
			}
		}
		return bookFileNames;
	}

	private void addDescription(BookDescription description) {
		if (description == null) {
			return;
		}

		final Author author = description.getAuthor();
		ArrayList books = (ArrayList)myCollection.get(author);
		if (books == null) {
			books = new ArrayList();
			myCollection.put(author, books);
			myAuthors.add(author);
		}
		books.add(description);
	}
	
	private class DescriptionComparator implements Comparator {
		public int compare(Object descr1, Object descr2) {
			BookDescription d1 = (BookDescription)descr1;
			BookDescription d2 = (BookDescription)descr2;
			String sequenceName1 = d1.getSequenceName();
			String sequenceName2 = d2.getSequenceName();
				
			if ((sequenceName1.length() == 0) && (sequenceName2.length() == 0)) {
				return d1.getTitle().compareTo(d2.getTitle());
			}
			if (sequenceName1.length() == 0) {
				return d1.getTitle().compareTo(sequenceName2);
			}
			if (sequenceName2.length() == 0) {
				return sequenceName1.compareTo(d2.getTitle());
			}
			if (!sequenceName1.equals(sequenceName2)) {
				return sequenceName1.compareTo(sequenceName2);
			}
			return (d1.getNumberInSequence() - d2.getNumberInSequence());
		}
	}
	
	static public class LastOpenedBooks {
		public ZLIntegerRangeOption MaxListSizeOption;
		static private final String GROUP = "LastOpenedBooks";
		static private final String BOOK = "Book";
		private final ArrayList/*BookDescription*/ myBooks = new ArrayList();

		public LastOpenedBooks() {
			MaxListSizeOption = new ZLIntegerRangeOption(ZLOption.STATE_CATEGORY, GROUP, "MaxSize", 1, 100, 10);
			final int size = MaxListSizeOption.getValue();
			for (int i = 0; i < size; ++i) {
				String num = BOOK;
				num += i;
				String name = new ZLStringOption(ZLOption.STATE_CATEGORY, GROUP, num, "").getValue();
				if (name.length() != 0) {
					BookDescription description = BookDescription.getDescription(name);
					if (description != null) {
						myBooks.add(description);
					}
				}
			}
		}
			
		public	void addBook(String fileName) {
			for (int i = 0; i < myBooks.size(); i++) {
				if (((BookDescription)(myBooks.get(i))).getFileName().equals(fileName)) {
					myBooks.remove(myBooks.get(i));
					break;
				}
			}
			
			BookDescription description = BookDescription.getDescription(fileName);
			if (description != null) {
				myBooks.add(0, description);
			}
			
			final int maxSize = MaxListSizeOption.getValue();
			while (myBooks.size() > maxSize) {
				myBooks.remove(myBooks.size() - 1);
			}
			save();
		}
		
		public	ArrayList/*BookDescription*/ books() {
			return myBooks;
		}
		
		public void save() {
			int size = Math.min(MaxListSizeOption.getValue(), myBooks.size());
			for (int i = 0; i < size; ++i) {
				String num = BOOK;
				num += i;
				new ZLStringOption(ZLOption.STATE_CATEGORY, GROUP, num, "").setValue(((BookDescription)myBooks.get(i)).getFileName());
			}
		}
	}
}