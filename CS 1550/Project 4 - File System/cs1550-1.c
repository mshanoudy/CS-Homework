/*
	FUSE: Filesystem in Userspace
	Copyright (C) 2001-2007  Miklos Szeredi <miklos@szeredi.hu>

	This program can be distributed under the terms of the GNU GPL.
	See the file COPYING.

*/

#define	FUSE_USE_VERSION 26

#include <fuse.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <fcntl.h>

#define	BLOCK_SIZE 512  // Size of a disk block
#define	MAX_FILENAME 8  // Size of filename
#define	MAX_EXTENSION 3 // Size of file extension

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
	int nFiles;			            // How many files are in this directory.
                                    // Needs to be less than MAX_FILES_IN_DIR

	struct cs1550_file_directory
	{
		char fname[MAX_FILENAME + 1];   // Filename (plus space for nul)
		char fext[MAX_EXTENSION + 1];	// Extension (plus space for nul)
		size_t fsize;			        // File size
		long nStartBlock;               // Where the first block is on disk
	} files[MAX_FILES_IN_DIR];          // There is an array of these
};

typedef struct cs1550_directory_entry cs1550_directory_entry;

struct cs1550_disk_block
{
	// And all of the space in the block can be used for actual data storage
	char data[MAX_DATA_IN_BLOCK];
};

typedef struct cs1550_disk_block cs1550_disk_block;

/* -------------------------------------------------------------------------- */

/*
 * Gets the directory at the specified index
 * Returns 0 on success, -1 on failure
 */
static int get_directory(cs1550_directory_entry *currentDir, int offset)
{
    int res = -1;
    
    FILE *file = fopen(".directories", "rb");
    if (file == NULL)
        return res; // Error opening file
    if (fseek(file, sizeof(cs1550_directory_entry) * offset, SEEK_SET) == -1)
        return res; // Error moving file pointer
    if (fread(currentDir, sizeof(cs1550_directory_entry), 1, file))
        res = 0;    // Successfully read from file
    fclose(file);
    
    return res;
}

/*
 * Gets the index of a specified directory
 * Returns the index on success, -1 on failure
 */
static int get_directory_index(char *directory)
{
    int res = -1;
    
    cs1550_directory_entry currentDir;
    int index = 0;
    
    while (get_directory(&currentDir, index) != -1)
    {// Go through the directories list looking for the directory
        if (strcmp(directory, currentDir.dname) == 0)
            return index; // Match found
        index++;
    }
    
    return res;
}

/* -------------------------------------------------------------------------- */

/*
 * Called whenever the system wants to know the file attributes, including
 * simply whether the file exists or not. 
 *
 * man -s 2 stat will show the fields of a stat structure
 */
static int cs1550_getattr(const char *path, struct stat *stbuf)
{
	int res = 0;

	memset(stbuf, 0, sizeof(struct stat));
   
	// Is path the root dir?
	if (strcmp(path, "/") == 0)
    {
		stbuf->st_mode = S_IFDIR | 0755;
		stbuf->st_nlink = 2;
	}
    else
    {// Split the path and find index
        char directory[MAX_FILENAME + 1],filename[MAX_FILENAME + 1], extension[MAX_EXTENSION + 1];
        //split_path(path, directory, filename, extension);
        sscanf(path, "/%[^/]/%[^.].%s", directory, filename, extension);
        int index = get_directory_index(directory); // Find the index
        
        if (index != -1)
        {// Valid directory path
            if (sizeof(filename) == 0)
            {// Subdirectory
                stbuf->st_mode = S_IFDIR | 0755;
                stbuf->st_nlink = 2;
                res = 0; // No error
            }
            else
            {// File
                // Get directory info
                cs1550_directory_entry currentDir;
                if (get_directory(&currentDir, index) == -1)
                    return -ENOENT; // Error getting directory info
                
                // Find the file
                int x;
                for (x = 0; x < currentDir.nFiles; x++)
                    if (strcmp(currentDir.files[x].fname, filename) == 0 &&
                        strcmp(currentDir.files[x].fext, extension) == 0)
                    {
                        stbuf->st_mode = S_IFREG | 0666;
                        stbuf->st_nlink = 1; // File links
                        stbuf->st_size = currentDir.files[x].fsize;
                        res = 0; // No error
                    }
            }
        }// Invalid path
        else
            res = -ENOENT;
	}
    
	return res;
}

/* 
 * Called whenever the contents of a directory are desired. Could be from an 'ls'
 * or could even be when a user hits TAB to do autocompletion
 */
static int cs1550_readdir(const char *path, void *buf, fuse_fill_dir_t filler,
                          off_t offset, struct fuse_file_info *fi)
{
    int res = -ENOENT;
    
    cs1550_directory_entry currentDir;
    
    // Since we're building with -Wall (all warnings reported) we need
	// to "use" every parameter, so let's just cast them to void to
	// satisfy the compiler
	(void) offset;
	(void) fi;
    
    // The root directory
    if (strcmp(path, "/") == 0)
    {
        filler(buf, ".", NULL, 0);
        filler(buf, "..", NULL, 0);
        
        // List any subdirectories
        int x = 0;
        while (get_directory(&currentDir, x) != -1)
        {
            filler(buf, currentDir.dname, NULL, 0);
            x++;
        }
        
        res = 0; // No error
    }
    // Not the root directory
    else
    {
        char directory[MAX_FILENAME + 1],filename[MAX_FILENAME + 1], extension[MAX_EXTENSION + 1];
        //split_path(path, directory, filename, extension);
        sscanf(path, "/%[^/]/%[^.].%s", directory, filename, extension);
        int index = get_directory_index(directory); // Find the index
        
        // Valid index
        if (index != -1)
            if (get_directory(&currentDir, index) != -1)
            {
                filler(buf, ".", NULL, 0);
                filler(buf, "..", NULL, 0);
                
                char filepath[MAX_FILENAME + MAX_EXTENSION + 2];
                
                // Add filesnames in this directory to buffer
                int x;
                for (x = 0; x < currentDir.nFiles; x++)
                {
                    sprintf(filepath, "%s.%s", currentDir.files[x].fname, currentDir.files[x].fext);
                    filler(buf, filepath + 1, NULL, 0); // The +1 skips the leading '/' on the filenames
                }
                
                res = 0; // No error
            }
    }
    
	return res;
}

/* 
 * Creates a directory. We can ignore mode since we're not dealing with
 * permissions, as long as getattr returns appropriate ones for us.
 */
static int cs1550_mkdir(const char *path, mode_t mode)
{
    int res = 0;
    
    (void) path;
	(void) mode; // Ignore mode
    
    
    char directory[MAX_FILENAME + 1],filename[MAX_FILENAME + 1], extension[MAX_EXTENSION + 1];
    
    sscanf(path, "/%[^/]/%[^.].%s", directory, filename, extension);

    if (1)
    {
        int index = get_directory_index(directory); // Find the index
        if (index != -1)
            res = -EEXIST;       // New directory already exists
        else if (strlen(directory) > MAX_FILENAME)
            res = -ENAMETOOLONG; // New directory name is too long
        else
        {
            cs1550_directory_entry currentDir;
            strcpy(currentDir.dname, directory);
            currentDir.nFiles = 0;
            
            // Add new directory to .directories file
            FILE *file = fopen(".directories", "ab");
            fwrite(&currentDir, sizeof(cs1550_directory_entry), 1, file);
            fclose(file);
        }
    }
    else
        res = -EPERM; // New directory not under root dir only
    
	return res;
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
	(void) path;
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

	// Check to make sure path exists
	// Check that size is > 0
	// Check that offset is <= to the file size
	// Read in data
	// Set size and return, or error

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
	(void) buf;
	(void) offset;
	(void) fi;
	(void) path;

	// Check to make sure path exists
	// Check that size is > 0
	// Check that offset is <= to the file size
	// Write data
	// Set size (should be same as input) and return, or error

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
    .mkdir	= cs1550_mkdir,
	.rmdir = cs1550_rmdir,
    .read	= cs1550_read,
    .write	= cs1550_write,
	.mknod	= cs1550_mknod,
	.unlink = cs1550_unlink,
	.truncate = cs1550_truncate,
	.flush = cs1550_flush,
	.open	= cs1550_open,
};

//Don't change this.
int main(int argc, char *argv[])
{
	return fuse_main(argc, argv, &hello_oper, NULL);
}
