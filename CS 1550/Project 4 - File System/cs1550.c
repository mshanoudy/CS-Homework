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

#define	BLOCK_SIZE 512	// Size of a disk block
#define	MAX_FILENAME 8	// Size of filename
#define	MAX_EXTENSION 3	// Size of file extension

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
{
	// And all of the space in the block can be used for actual data storage.
	char data[MAX_DATA_IN_BLOCK];
};

typedef struct cs1550_disk_block cs1550_disk_block;

/*--------------------------------------------------------------------------------*/


/*
* Sets the passed directory entry and returns the index of the directory on success
*/
static int get_directory(char *directory, cs1550_directory_entry *thisDirectory)
{
	cs1550_directory_entry currentDirectory;
	memset(&currentDirectory, 0, sizeof(cs1550_directory_entry));

	FILE *file = fopen(".directories", "ab+");
	if (file == NULL) 
		return -1;

	int index = 0;
	while (fread(&currentDirectory, sizeof(cs1550_directory_entry), 1, file) == 1)
	{
		// Is this the directory?
		if (strcmp(currentDirectory.dname, directory) == 0)
		{
			*thisDirectory = currentDirectory;
			fclose(file);
			return index;	
		}
		index++;
	}
	fclose(file);

	return -1;
}

/*
* Sets the passed directory entry and returns the index of the file on success
*/
static int get_file(char *directory, char *filename, char *extension, cs1550_directory_entry *thisDirectory)
{
	cs1550_directory_entry currentDirectory;
	memset(&currentDirectory, 0, sizeof(cs1550_directory_entry));

	FILE *file = fopen(".directories", "ab+");
	if (file == NULL) 
		return -1;

	while (fread(&currentDirectory, sizeof(cs1550_directory_entry), 1, file) == 1)
		// Is this the directory?
		if (strcmp(currentDirectory.dname, directory) == 0)
			// Is this directory empty?
			if (currentDirectory.nFiles > 0)
			{
				int index;
				for (index = 0; index < currentDirectory.nFiles; index++)
					if (strcmp(currentDirectory.files[index].fname, filename) == 0 &&
						strcmp(currentDirectory.files[index].fext, extension) == 0)
					{
						*thisDirectory = currentDirectory;
						fclose(file);
						return index;
					}
			}
	fclose(file);

	return -1;
}

/*
* Finds first free block and allocates it, returns that block's position
* TODO: As of now, each file will take up an entire block. This needs to be changed
*/
static int create_block()
{
	int index = 0;
	int bookKeeper = 0;

	FILE *file = fopen(".disk", "rb+");	
	if (file == NULL) 
		return -1;

	while (fread(&bookKeeper, sizeof(int), 1, file) == 1)
	{
		// Found an empty block
		if (bookKeeper == 0)
		{
			bookKeeper = 1;
			fwrite(&bookKeeper, sizeof(int), 1, file);
			fclose(file);
			return index;
		}

		fseek(file, -sizeof(int), SEEK_CUR); 			  
		if (fseek(file, sizeof(cs1550_disk_block), SEEK_CUR) != 0)
		{// Failed to seek to next block
			fclose(file);
			return -1;
		}

		index++;
	}
	fclose(file);

	return -1;
}

static int write_directory(cs1550_directory_entry *currentDirectory, int index)
{
	FILE *file = fopen(".disk", "wb+");	
	if (file == NULL) 
		return -1;	

	fseek(file, index * sizeof(cs1550_directory_entry), SEEK_SET);
	fwrite(currentDirectory, sizeof(cs1550_directory_entry), 1, file); // Needs error handling
	fclose(file);

	return 0;
}

static int write_block(cs1550_disk_block *block, int index)
{
	FILE *file = fopen(".disk", "wb+");	
	if (file == NULL) 
		return -1;	

	fseek(file, (index * sizeof(cs1550_disk_block)) + sizeof(int), SEEK_SET);
	fwrite(block, sizeof(cs1550_disk_block) - sizeof(int), 1, file); // Needs error handling
	fclose(file);

	return 0;
}

/*--------------------------------------------------------------------------------*/

/*
 * Called whenever the system wants to know the file attributes, including
 * simply whether the file exists or not. 
 *
 * man -s 2 stat will show the fields of a stat structure
 */
static int cs1550_getattr(const char *path, struct stat *stbuf)
{
	memset(stbuf, 0, sizeof(struct stat));
   
	// Is path the root dir?
	if (strcmp(path, "/") == 0) 
	{
		stbuf->st_mode  = S_IFDIR | 0755;
		stbuf->st_nlink = 2;
	} 
	else 
	{
		cs1550_directory_entry currentDirectory;
		memset(&currentDirectory, 0, sizeof(cs1550_directory_entry));

		char directory[MAX_FILENAME + 1], filename[MAX_FILENAME + 1], extension[MAX_EXTENSION + 1];
		sscanf(path, "/%[^/]/%[^.].%s", directory, filename, extension); 

		// Is this a valid directory?
		if (get_directory(directory, &currentDirectory) != -1)
		{
			// Is there something in this directory?
			if (currentDirectory.nFiles > 0)
			{
				int index = get_file(directory, filename, extension, &currentDirectory);
				// Is this a valid file?
				if (index != -1)
				{
					stbuf->st_mode = S_IFREG | 0666; 
					stbuf->st_nlink = 1; 	// file links
					stbuf->st_size = currentDirectory.files[index].fsize;
				}
			}
			else
			{
				stbuf->st_mode = S_IFDIR | 0755;
				stbuf->st_nlink = 2;
				stbuf->st_size = BLOCK_SIZE;
			}
		}
		else
			return -ENOENT;		
	}

	return 0;
}

/* 
 * Called whenever the contents of a directory are desired. Could be from an 'ls'
 * or could even be when a user hits TAB to do autocompletion
 */
static int cs1550_readdir(const char *path, void *buf, fuse_fill_dir_t filler,
			 				off_t offset, struct fuse_file_info *fi)
{
	//Since we're building with -Wall (all warnings reported) we need
	//to "use" every parameter, so let's just cast them to void to
	//satisfy the compiler
	(void) offset;
	(void) fi;

	perror("readdir called!");

	cs1550_directory_entry currentDirectory;
	memset(&currentDirectory, 0, sizeof(cs1550_directory_entry));

	// Is this root?
	if (strcmp(path, "/") == 0)
	{
		filler(buf, ".", NULL, 0);
		filler(buf, "..", NULL, 0);

		FILE *file = fopen(".directories", "ab+");
		if (file == NULL) 
			return -1;
		// List the subdirectories
		while (fread(&currentDirectory, sizeof(cs1550_directory_entry), 1, file) == 1)
			filler(buf, currentDirectory.dname, NULL, 0);
		fclose(file);
	}
	else
	{	
		char directory[MAX_FILENAME + 1], filename[MAX_FILENAME + 1], extension[MAX_EXTENSION + 1];
		sscanf(path, "/%[^/]/%[^.].%s", directory, filename, extension); 

		// Is this a valid path?
		if (get_directory(directory, &currentDirectory) != -1)
		{
			filler(buf, ".", NULL, 0);
			filler(buf, "..", NULL, 0);

			char filepath[MAX_FILENAME + MAX_EXTENSION + 3];
			// List the files
			int index;
			for (index = 0; index < currentDirectory.nFiles; index++)
			{
				sprintf(filepath, "%s.%s", currentDirectory.files[index].fname, currentDirectory.files[index].fext);
                filler(buf, filepath + 1, NULL, 0); // The +1 skips the leading '/' on the filenames
			}
		}
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
	(void) mode;

	cs1550_directory_entry currentDirectory;
	memset(&currentDirectory, 0, sizeof(cs1550_directory_entry));

	char directory[MAX_FILENAME + 1], filename[MAX_FILENAME + 1], extension[MAX_EXTENSION + 1];
	sscanf(path, "/%[^/]/%[^.].%s", directory, filename, extension); 

	if (strlen(directory) > MAX_FILENAME)
		return -ENAMETOOLONG;
	if (filename != NULL)
		return -EPERM;
	if (get_directory(directory, &currentDirectory) != -1)
		return -EEXIST;

	FILE *file = fopen(".directories", "ab+");
	if (file == NULL) 
		return -1;

	memset(&currentDirectory, 0, sizeof(cs1550_directory_entry));
	strcpy(currentDirectory.dname, directory);
	currentDirectory.nFiles = 0;
	fwrite(&currentDirectory, sizeof(cs1550_directory_entry), 1, file);
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
	(void) mode;
	(void) dev;

	cs1550_directory_entry currentDirectory;
	memset(&currentDirectory, 0, sizeof(cs1550_directory_entry));

	char directory[MAX_FILENAME + 1], filename[MAX_FILENAME + 1], extension[MAX_EXTENSION + 1];
	sscanf(path, "/%[^/]/%[^.].%s", directory, filename, extension); 

	if (strlen(filename) > MAX_FILENAME || strlen(extension) > MAX_EXTENSION)
		return -ENAMETOOLONG;
	if (directory == NULL)
		return -EPERM;
	if (get_file(directory, filename, extension, &currentDirectory) != -1)
		return -EEXIST;

	// Get the directory
	int directoryIndex = get_directory(directory, &currentDirectory);
	int fileIndex = currentDirectory.nFiles - 1;
	if (fileIndex == -1) 
		fileIndex = 0;
	
	// Update directory entry info
	currentDirectory.nFiles = currentDirectory.nFiles + 1;
	currentDirectory.files[fileIndex].fname = filename;
	currentDirectory.files[fileIndex].fext = extension;
	currentDirectory.files[fileIndex].fsize = 0;
	currentDirectory.files[fileIndex].nStartBlock = create_block(); // TODO: Need to account for error handling if full

	// Update .directories
	write_directory(&currentDirectory, directoryIndex);

	return 0;
}

/*
 * Deletes a file
 */
static int cs1550_unlink(const char *path)
{
    (void) path;

    return 0;
}

/* 
 * Read size bytes from file into buf starting from offset
 *
 */
static int cs1550_read(const char *path, char *buf, size_t size, off_t offset,
			  		   struct fuse_file_info *fi)
{
	(void) buf;
	(void) offset;
	(void) fi;
	(void) path;

	//check to make sure path exists
	//check that size is > 0
	//check that offset is <= to the file size
	//read in data
	//set size and return, or error

	size = 0;

	return size;
}

/* 
 * Write size bytes from buf into file starting from offset
 *
 */
static int cs1550_write(const char *path, const char *buf, size_t size, 
			  			off_t offset, struct fuse_file_info *fi)
{
	(void) fi;

	cs1550_directory_entry currentDirectory;
	memset(&currentDirectory, 0, sizeof(cs1550_directory_entry));
	cs1550_disk_block currentBlock;
	memset(&currentBlock, 0, sizeof(cs1550_disk_block));

	char directory[MAX_FILENAME + 1], filename[MAX_FILENAME + 1], extension[MAX_EXTENSION + 1];
	sscanf(path, "/%[^/]/%[^.].%s", directory, filename, extension); 

	int fileIndex      = get_file(directory, filename, extension, &currentDirectory);
	int directoryIndex = get_directory(directory, &currentDirectory);

	// Check to make sure path exists
	if (fileIndex == -1)
		return -EBADF;
	// Check that size is > 0
	if (size <= 0)
		return -1;
	// Check that offset is <= to the file size
	if (offset > size || size > MAX_DATA_IN_BLOCK)
		return -EFBIG;

	// Write data
	currentBlock.data = *buf;
	write_block(&currentBlock, currentDirectory.files[fileIndex].nStartBlock);

	// Set size (should be same as input) and return, or error
	currentDirectory.files[fileIndex].fsize = size;
	write_directory(&currentDirectory, directoryIndex);

	return size;
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
	.rmdir		= cs1550_rmdir,
    .read		= cs1550_read,
    .write		= cs1550_write,
	.mknod		= cs1550_mknod,
	.unlink		= cs1550_unlink,
	.truncate	= cs1550_truncate,
	.flush		= cs1550_flush,
	.open		= cs1550_open,
};

//Don't change this.
int main(int argc, char *argv[])
{
	return fuse_main(argc, argv, &hello_oper, NULL);
}
