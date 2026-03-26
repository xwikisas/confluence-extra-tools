# Confluence space exporter

This script aims at being run on the Confluence server holding the spaces that need to be exported. The script will request the export of several spaces in the Confluence instance, and will collect each space export into a dedicated directory.

## Pre-requisites

### Software dependencies

Export scripts are written in Python, in order to run them, you will need the following commands :
* `python` ; you should have Python 3 at least (Python 2 is not supported) ; check your version of python by running `python --version`
* `virtualenv`

You should be able to install this software by running the following on a Debian-based GNU/Linux operating system :

```
sudo apt install python3-virtualenv
```

### Script evironment setup

Copy files `export.py` and `requirements.txt` to a directory on the Confluence server where the export should occur.

* Navigate to the directory where these files have been copied to
* Create a virtual environment : `virtualenv venv`
* Activate the environment : `source venv/bin/activate`
* Install python dependencies : `pip install -r requirements.txt`

### Authentication setup

In order to function properly, this script will require a local Confluence account with administrator privileges.

## Performing an export

### Prepare for the export

* Create a file `batch_input.txt` that will hold the list of spaces which need to be exported. Each space should be on a new line.
* Make sure that the user that will run the export script has read access to the temporary directory used by Confluence.
* Create a new directory that will be used to collect each exported space. This directory can be located anywhere on the server, but the user that will run the export script should have read and write access to this directory.

### Set the parameters of the export

Edit the file `export_batch.sh` and set the variables as follows (replace xxx with the values):

* Set the environment variable `CONFLUENCE_USER` with the username of the local administrator account that will be used to trigger the exports
* Set the environment variable `CONFLUENCE_PWD` with the password of the local administrator account that will be used to trigger the exports
* Set the environment variable `CONFLUENCE_BASE_URL` with the base URL to the Confluence instance. If the Confluence site uses a context (such as `/wiki` or `/confluence`, do include the particle in this URL as well)
* Set the environment variable `CONFLUENCE_TEMP_DIR` with the path of the temporary directory used by the Confluence instance
* Set the environment variable `CONFLUENCE_EXPORT_DIR` with the path of the export directory that will be used to collect the exports

### Perform the export

In the shell in which you will run the export:

* Activate the python environment: `source venv/bin/activate`
* Ensure the user has execution rights on the shell script: `chmod u+x export_batch.sh`
* Run the export script: `./export_batch.sh`

Executing the script will also result in the creation of the file `spaces.csv`, that will list each space exported, with information about the number of pages found, the number of blog entries found as well as the path to the exported file.
