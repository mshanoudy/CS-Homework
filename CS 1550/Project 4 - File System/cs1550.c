/*
	FUSE: Filesystem in Userspace
	Copyright (C) 2001-2007  Miklos Szeredi <miklos@szeredi.hu>

	This program can be distributed under the terms of the GNU GPL.
	See the file COPYING.

*/

#define	FUSE_USE_VERSION 26

#include <fuse.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <fcntl.h>

#define	BLOCK_SIZE 512 	 // Size of a disk block				
#define	MAX_FILENAME 8	 // Size of filename
#define	MAX_EXTENSION 3	 // Size of extension

// How many files can there be in one directory?
#define	MAX_FILES_IN_DIR (BLOCK_SIZE - (MAX_FILENAME + 1) - sizeof(int)) / \
	((MAX_FILENAME + 1) + (MAX_EXTENSION + 1) + sizeof(size_t) + sizeof(long))

// How much data can one block hold?
#define	MAX_DATA_IN_BLOCK BLOCK_SIZE

// How many pointers in an inode?
#define NUM_POINTERS_IN_INODE ((BLOCK_SIZE - sizeof(unsigned int) - sizeof(unsigned long)) / sizeof(unsigned long))

struct cs1550_directory_entry
{
	char dname[MAX_FILENAME	+ 1];	// The directory name (plus space for a nul)
	int nFiles;						// How many files are in this directory. 
									// Needs to be less than MAX_FILES_IN_DIR

	struct cs1550_file_directory
	{
		char fname[MAX_FILENAME + 1];	// Filename (plus space for nul)
		char fext[MAX_EXTENSION + 1];	// Extension (plus space for nul)
		size_t fsize;					// File size
		long nStartBlock;				// Where the first block is on disk
	} files[MAX_FILES_IN_DIR];			// There is an array of these
};

typedef struct cs1550_directory_entry cs1550_directory_entry;

struct cs1550_disk_block
{// And all of the space in the block can be used for actual data storage
	char data[MAX_DATA_IN_BLOCK];
};

typedef struct cs1550_disk_block cs1550_disk_block;

/*----------------------------------------------------------------------------*/

/*
 * Split up the path into seperate variables
 * Returns the number of variables filled
 */
static int split_path(const char *path, char *directory, char *filename, char *extension)
{
	return sscanf(path, "/%[^/]/%[^.].%s", directory, filename, extension);
}

/*
 * Finds the desired directory
 * Reutrns Index of directory and sets currentDirectory to said directory_entry
 */
static int get_directory(char *directory, cs1550_directory_entry *currentDirectory)
{
	cs1550_directory_entry tempDirectory;
	memset(&tempDirectory, 0, sizeof(cs1550_directory_entry));

	FILE *file = fopen(".directories", "ab+");
	if (file == NULL) 
	{
		perror("** fopen failed in get_directory **");
		return -1;
	}

	int directoryIndex = 0;

	while (fread(&tempDirectory, sizeof(cs1550_directory_entry), 1, file) == 1)
	{
		// Is this the directory we're looking for?
		if (strcmp(tempDirectory.dname, directory) == 0)
		{
			*currentDirectory = tempDirectory;
			fclose(file);
			return directoryIndex;	
		}
		directoryIndex++;
	}
	fclose(file);

	return -1;
}

/*
 * Finds the desired file's index within a directory
 * Returns index of file
 */
static int get_file_index(char *filename, char *extension, cs1550_directory_entry currentDirectory)
{
	int fileIndex;

	for (fileIndex = 0; fileIndex < MAX_FILES_IN_DIR; fileIndex++)
		if (strcmp(currentDirectory.files[fileIndex].fname, filename) == 0 &&
			strcmp(currentDirectory.files[fileIndex].fext, extension) == 0)
				return fileIndex;

	return -1;	
}

/*----------------------------------------------------------------------------*/

/*
 * Called whenever the system wants to know the file attributes, including
 * simply whether the file exists or not. 
 *
 * man -s 2 stat will show the fields of a stat structure
 */
static int cs1550_getattr(const char *path, struct stat *stbuf)
{
	perror("** cs1550_getattr called **");

	char directory[MAX_FILENAME + 1], filename[MAX_FILENAME + 1], extension[MAX_EXTENSION + 1];
	int  pathElements = split_path(path, directory, filename, extension);
	int  res = 0;
	
	// Clear the stat struct
	memset(stbuf, 0, sizeof(struct stat));
   
	// Is path the root dir?
	if (strcmp(path, "/") == 0) 
	{
		stbuf->st_mode  = S_IFDIR | 0755;
		stbuf->st_nlink = 2;
	} 
	else
	// Not the root directory 
	{
		cs1550_directory_entry currentDirectory;

		// Is this a path to a valid directory?
		if (get_directory(directory, &currentDirectory) != -1)
		{
			// Is this a path to a directory or file?
			if (pathElements < 3)
			{
				stbuf->st_mode  = S_IFDIR | 0755;
				stbuf->st_nlink = 2;
				res = 0;	
			}
			// Path to a file
			else
			{
				int fileIndex = get_file_index(filename, extension, currentDirectory);

				// Does this file exist?
				if (fileIndex != -1)
				{
					stbuf->st_mode  = S_IFREG | 0666; 
					stbuf->st_nlink = 1; 	
					stbuf->st_size  = currentDirectory.files[fileIndex].fsize;
					res = 0;
				}
				// New file
				else
				{
					perror("** New file condition in cs1550_getattr **");
					stbuf->st_mode  = S_IFREG | 0666; 
					stbuf->st_nlink = 1; 	
					stbuf->st_size  = 1;
					res = 0;
					// TODO: Handle this if need be
				}
			}
		}
		else
			res = -ENOENT;
	}

	return res;
}

/* 
 * Called whenever the contents of a directory are desired. Could be from an 'ls'
 * or could even be when a user hits TAB to do autocompletion
 */
static int cs1550_readdir(const char *path, void *buf, fuse_fill_dir_t filler, off_t offset, struct fuse_file_info *fi)
{
	perror("** cs1550_readdir called **");

	char directory[MAX_FILENAME + 1], filename[MAX_FILENAME + 1], extension[MAX_EXTENSION + 1];
	int  pathElements = split_path(path, directory, filename, extension);
	
	cs1550_directory_entry currentDirectory;

	// Unused parameters
	(void) offset;
	(void) fi;

	// Is path the root dir?
	if (strcmp(path, "/") == 0)
	{
		filler(buf, ".", NULL, 0);
		filler(buf, "..", NULL, 0);	

		FILE *file = fopen(".directories", "ab+");
		if (file == NULL) 
		{
			perror("** fopen failed in cs1550_readdir **");
			return -1;
		}		
		// List the subdirectories
		while (fread(&currentDirectory, sizeof(cs1550_directory_entry), 1, file) == 1)
			filler(buf, currentDirectory.dname, NULL, 0);
		fclose(file);		
	}
	// Not the root directory
	else
	{
		// Is this a valid directory?
		if (get_directory(directory, &currentDirectory) != -1)
		{
			filler(buf, ".", NULL, 0);
			filler(buf, "..", NULL, 0);

			perror("** Printing files in directory in cs1550_readdir **");

			int fileIndex;
			// List directory contents
			for (fileIndex = 0; fileIndex < currentDirectory.nFiles; fileIndex++)
			{
				char fullFileName[MAX_FILENAME + MAX_EXTENSION + 4];
				strcpy(fullFileName, currentDirectory.files[fileIndex].fname);
				strcat(fullFileName, ".");
				strcat(fullFileName, currentDirectory.files[fileIndex].fext);

				filler(buf, fullFileName, NULL, 0);	// +1 skips the leading '/' on the filenames
			}
		}
		// Invalid directory path
		else
			return -ENOENT;
	}
	
	return 0;
}

/* 
 * Creates a directory. We can ignore mode since we're not dealing with
 * permissions, as long as getattr returns appropriate ones for us.
 */
static int cs1550_mkdir(const char *path, mode_t mode)
{
	char directory[MAX_FILENAME + 1], filename[MAX_FILENAME + 1], extension[MAX_EXTENSION + 1];
	int  pathElements = split_path(path, directory, filename, extension);

	cs1550_directory_entry currentDirectory;

	(void) mode; // Unused parameter

	// Is this directory name too long?
	if (strlen(directory) > MAX_FILENAME)
		return -ENAMETOOLONG;
	// Is path not under the root dir only?
	if (pathElements > 1)
		return -EPERM;
	// Does this directory already exist?
	if (get_directory(directory, &currentDirectory) != -1)
		return -EEXIST;

	memset(&currentDirectory, 0, sizeof(cs1550_directory_entry)); // Clear the directory_entry

	// Set directory_entry fields
	strcpy(currentDirectory.dname, directory);
	currentDirectory.nFiles = 0;

	// Write to .directories
	FILE *file = fopen(".directories", "ab+");
	if (file == NULL) 
	{
		perror("** fopen failed in cs1550_mkdir **");
		return -EIO;
	}		
	if (fwrite(&currentDirectory, sizeof(cs1550_directory_entry), 1, file) != 1)
	{
		perror("** fwrite failed in cs1550_mkdir **");
		return -EIO;
	}
	fclose(file);

	return 0;
}

/* 
 * Removes a directory.
 */
static int cs1550_rmdir(const char *path)
{
	(void) path;
    return 0;
}

/* 
 * Does the actual creation of a file. Mode and dev can be ignored.
 *
 */
static int cs1550_mknod(const char *path, mode_t mode, dev_t dev)
{
	char directory[MAX_FILENAME + 1], filename[MAX_FILENAME + 1], extension[MAX_EXTENSION + 1];
	int  pathElements = split_path(path, directory, filename, extension);

	cs1550_directory_entry currentDirectory;
	int directoryIndex = get_directory(directory, &currentDirectory);

	// Unused parameters
	(void) mode;
	(void) dev;

	perror("** cs1550_mknod called **");

	// Is the filename or extension too long?
	if (strlen(filename) > MAX_FILENAME || strlen(extension) > MAX_EXTENSION)
		return -ENAMETOOLONG;
	// Is file being created in the root dir?
	if (pathElements < 3)
		return -EPERM;
	// Does this file already exist?
	if (get_file_index(filename, extension, currentDirectory) != -1)
		return -EEXIST;

	int fileIndex = currentDirectory.nFiles;	
	
	// Update the directory_entry fields
	currentDirectory.nFiles = currentDirectory.nFiles + 1;
	strcpy(currentDirectory.files[fileIndex].fname, filename);
	strcpy(currentDirectory.files[fileIndex].fext, extension);
	currentDirectory.files[fileIndex].fsize = 0;
	currentDirectory.files[fileIndex].nStartBlock = 0;	// TODO: make this not 0

	long int directoryOffset = directoryIndex * sizeof(cs1550_directory_entry);

	// Write to .directories
	FILE *file = fopen(".directories", "rb+");
	if (file == NULL) 
	{
		perror("** fopen failed in cs1550_mknod **");
		return -EIO;
	}	
	if (fseek(file, directoryOffset, SEEK_SET) != 0)
	{
		perror("** fseek failed in cs1550_mknod **");
		return -EIO;
	}
	if (fwrite(&currentDirectory, sizeof(cs1550_directory_entry), 1, file) != 1)
	{
		perror("** fwrite failed in cs1550_mknod **");
		return -EIO;
	}
	fclose(file);

	return 0;
}

/* 
 * Write size bytes from buf into file starting from offset
 *
 */
static int cs1550_write(const char *path, const char *buf, size_t size, off_t offset, struct fuse_file_info *fi)
{
	char directory[MAX_FILENAME + 1], filename[MAX_FILENAME + 1], extension[MAX_EXTENSION + 1];
	int  pathElements = split_path(path, directory, filename, extension);

	cs1550_directory_entry currentDirectory;
	int directoryIndex = get_directory(directory, &currentDirectory);
	int fileIndex 	   = get_file_index(filename, extension, currentDirectory);

	(void) fi; // Unused parameter

	// Is this a valid path?
	if (directoryIndex == -1)
	{
		perror("** Invalid path in cs1550_write **");
		return -1;
	}
	// Is the size > 0?
	if (size <= 0)
	{
		perror("** Size not > 0 in cs1550_write **");
		return -1;
	}
	// Is the offset <= size?
	if (offset > size)
	{
		perror("** offset > size in cs1550_write **");
		return -EFBIG;
	}
		
	perror("** cs1550_write called **");
	perror(buf);
	
	int directoryOffset = directoryIndex * sizeof(cs1550_directory_entry);
	int fileOffset = 0;
	//int fileOffset 		= (currentDirectory.files[fileIndex].nStartBlock * currentDirectory.files[fileIndex].fsize) + offset;

	// Write to .disk
	FILE *file = fopen(".disk", "rb+");
	if (file == NULL)
	{
		perror("** fopen failed in cs1550_write **");
		return -EIO;
	}
	if (fseek(file, fileOffset, SEEK_SET) != 0)
	{
		perror("** fseek failed in cs1550_write");
		return -EIO;
	}
	if (fwrite(buf, size, 1, file) != 1)
	{
		perror("** fwrite failed in cs1550_write **");
		return -EIO;
	}
	fclose(file);

	fileIndex = currentDirectory.nFiles;
	
	perror("** wrote to .disk in cs1550_write **");
	if (currentDirectory.nFiles >= 0)
		perror("** nFiles is >= 0 in cs1550_write **");

	// Set directory entry
	currentDirectory.nFiles = currentDirectory.nFiles + 1;
	currentDirectory.files[0].fsize = size;
	strcpy(currentDirectory.files[0].fname, filename);
	strcpy(currentDirectory.files[0].fext, extension);
	currentDirectory.files[0].nStartBlock = 0;

	// Write to .directories 
	FILE *f = fopen(".directories", "rb+");
	if (file == NULL) 
	{
		perror("** fopen failed in cs1550_write **");
		return -EIO;
	}	
	if (fseek(f, directoryOffset, SEEK_SET) != 0)
	{
		perror("** fseek failed in cs1550_write **");
		return -EIO;
	}
	if (fwrite(&currentDirectory, sizeof(cs1550_directory_entry), 1, f) != 1)
	{
		perror("** fwrite failed in cs1550_write **");
		return -EIO;
	}
	fclose(f); 

	perror("** wrote to .directories in cs1550_write **");

	return size;
}

/* 
 * Read size bytes from file into buf starting from offset
 *
 */
static int cs1550_read(const char *path, char *buf, size_t size, off_t offset, struct fuse_file_info *fi)
{
	char directory[MAX_FILENAME + 1], filename[MAX_FILENAME + 1], extension[MAX_EXTENSION + 1];
	int  pathElements = split_path(path, directory, filename, extension);

	cs1550_directory_entry currentDirectory;
	int directoryIndex = get_directory(directory, &currentDirectory);
	int fileIndex 	   = get_file_index(filename, extension, currentDirectory);
	
	(void) fi; // Unused parameter

	perror("** cs1550_read called **");

	// Is this a valid path?
	if (fileIndex == -1)
	{
		perror("** Invalid path in cs1550_read **");
		return -1;
	}
	// Is the size > 0?
	if (size <= 0)
	{
		perror("** Size not > 0 in cs1550_read **");
		return -1;
	}
	// Is the offset <= size?
	if (offset > size)
	{
		perror("** offset > size in cs1550_read **");
		return -EFBIG;
	}
	// Is this path a directory?
	if (pathElements < 3)
	{
		perror("** Path is a directory in cs1550_read ");
		return -EISDIR;
	}
	
	int fileOffset = 0;

	// Read from .disk
	FILE *file = fopen(".disk", "rb+");
	if (file == NULL)
	{
		perror("** fopen failed in cs1550_read **");
		return -EIO;
	}
	if (fseek(file, fileOffset, SEEK_SET) != 0)
	{
		perror("** fseek failed in cs1550_read");
		return -EIO;
	}
	if (fread(buf, size, 1, file) != 1)
	{
		perror("** fread failed in cs1550_read **");
		return -EIO;
	}
	fclose(file);

	//read in data
	//set size and return, or error

	return size;
}

/*
 * Deletes a file
 */
static int cs1550_unlink(const char *path)
{
	char directory[MAX_FILENAME + 1], filename[MAX_FILENAME + 1], extension[MAX_EXTENSION + 1];
	int  pathElements = split_path(path, directory, filename, extension);

	cs1550_directory_entry currentDirectory;
	int directoryIndex = get_directory(directory, &currentDirectory);
	int fileIndex 	   = get_file_index(filename, extension, currentDirectory);

	// Is the path a directory?
	if (pathElements < 3)
	{
		perror("** path is a directory in cs1550_unlink");
		return -EISDIR;
	}    
	// Is this a valid file?
	if (fileIndex == -1)
	{
		perror("** file not found in cs1550_unlink");
		return -ENOENT;
	}

	int directoryOffset = directoryIndex * sizeof(cs1550_directory_entry);
	int fileOffset 		= currentDirectory.files[fileIndex].nStartBlock;

	// TODO: Mark block on .disk as empty

	// Update the directory_entry
	currentDirectory.nFiles = currentDirectory.nFiles - 1;
	strcpy(currentDirectory.files[fileIndex].fname, "");
	strcpy(currentDirectory.files[fileIndex].fext, "");
	currentDirectory.files[fileIndex].fsize = 0;

	// Write to .directories
	FILE *file = fopen(".directories", "rb+");
	if (file == NULL) 
	{
		perror("** fopen failed in cs1550_unlink **");
		return -EIO;
	}	
	if (fseek(file, directoryOffset, SEEK_SET) != 0)
	{
		perror("** fseek failed in cs1550_unlink **");
		return -EIO;
	}
	if (fwrite(&currentDirectory, sizeof(cs1550_directory_entry), 1, file) != 1)
	{
		perror("** fwrite failed in cs1550_unlink **");
		return -EIO;
	}
	fclose(file);

    return 0;
}

/******************************************************************************
 *
 *  DO NOT MODIFY ANYTHING BELOW THIS LINE
 *
 *****************************************************************************/

/*
 * truncate is called when a new file is created (with a 0 size) or when an
 * existing file is made shorter. We're not handling deleting files or 
 * truncating existing ones, so all we need to do here is to initialize
 * the appropriate directory entry.
 *
 */
static int cs1550_truncate(const char *path, off_t size)
{
	(void) path;
	(void) size;

    return 0;
}


/* 
 * Called when we open a file
 *
 */
static int cs1550_open(const char *path, struct fuse_file_info *fi)
{
	(void) path;
	(void) fi;
    /*
        //if we can't find the desired file, return an error
        return -ENOENT;
    */

    //It's not really necessary for this project to anything in open

    /* We're not going to worry about permissions for this project, but 
	   if we were and we don't have them to the file we should return an error

        return -EACCES;
    */

    return 0; //success!
}

/*
 * Called when close is called on a file descriptor, but because it might
 * have been dup'ed, this isn't a guarantee we won't ever need the file 
 * again. For us, return success simply to avoid the unimplemented error
 * in the debug log.
 */
static int cs1550_flush (const char *path , struct fuse_file_info *fi)
{
	(void) path;
	(void) fi;

	return 0; //success!
}


//register our new functions as the implementations of the syscalls
static struct fuse_operations hello_oper = {
    .getattr	= cs1550_getattr,
    .readdir	= cs1550_readdir,
    .mkdir		= cs1550_mkdir,
	.rmdir 		= cs1550_rmdir,
    .read		= cs1550_read,
    .write		= cs1550_write,
	.mknod		= cs1550_mknod,
	.unlink 	= cs1550_unlink,
	.truncate 	= cs1550_truncate,
	.flush 		= cs1550_flush,
	.open		= cs1550_open,
};

//Don't change this.
int main(int argc, char *argv[])
{
	return fuse_main(argc, argv, &hello_oper, NULL);
}
