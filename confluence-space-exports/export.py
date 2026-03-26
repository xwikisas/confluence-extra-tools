import requests
import json
import os
import csv

username = os.getenv("CONFLUENCE_USER")
password = os.getenv("CONFLUENCE_PWD")
domain = os.getenv("CONFLUENCE_BASE_URL")
temp_dir = os.getenv("CONFLUENCE_TEMP_DIR")
export_dir = os.getenv("CONFLUENCE_EXPORT_DIR")

csv_header = ['Space Key', 'Page Count', 'Blog pages Count', 'Exported File Path']
csv_outputfile_path = 'spaces.csv'

headers = {"Content-Type": "application/json", "Accept": "application/json"}

def get_blog_entries_count(space_key):
    total_blog_entries = 0
    payload = {
        "jsonrpc": "2.0",
        "method": "getBlogEntries",
        "params": [space_key],
        "id": space_key
    }
    request = requests.post(url, data=json.dumps(payload), auth=(username, password), headers=headers)
    if request.status_code == 200:
        data = request.json()
        if 'result' in data:
            total_blog_entries = len(data['result'])
            print(f"Blog entries count in {space_key} = {total_blog_entries}")
        else:
            print(f"Failed to get blog entries count in space [{space_key}]")
    else:
        print(f"Invalid response code [{request.status_code}] when getting blog entries count for [{space_key}]")
    
    return total_blog_entries

def get_pages_count(space_key):
    total_pages = 0
    payload = {
        "jsonrpc": "2.0",
        "method": "getPages",
        "params": [space_key],
        "id": space_key
    }
    request = requests.post(url, data=json.dumps(payload), auth=(username, password), headers=headers)
    if request.status_code == 200:
        data = request.json()
        if 'result' in data:
            total_pages = len(data['result'])
            print(f"Page count in {space_key} = {total_pages}")
        else:
            print(f"Failed to get page count in space [{space_key}]")
    else:
        print(f"Invalid response code [{request.status_code}] when getting page count for [{space_key}]")
    
    return total_pages

def export_space(space_key):
    total_pages = 0
    blog_pages = 0
    print(f"\nExporting Space = {space_key}")

    try:
        total_pages = get_pages_count(space_key)
        blog_pages = get_blog_entries_count(space_key)

        payload = {
            "jsonrpc": "2.0",
            "method": "exportSpace",
            "params": [space_key, "TYPE_XML", True],
            "id": space_key
        }
        payload_str = json.dumps(payload)
        res = requests.post(url, data=payload_str, auth=(username, password), headers=headers)

        if res.status_code == 200:
            data = res.json()

            if "result" in data:
                with open(csv_outputfile_path, mode='a', newline='') as file:
                    writer = csv.writer(file)
                    download_url = data['result']

                    print(f"Found download URL for [{space_key}] : [{download_url}]")
                    # Attempt to extract the file name
                    splitted_url = download_url.split('/')
                    file_name = splitted_url[len(splitted_url) - 1]

                    temp_location = f"{temp_dir}/{file_name}"
                    export_location = f"{export_dir}/{space_key}.zip"

                    try:
                        os.rename(temp_location, export_location)
                        writer.writerows([[space_key, total_pages, blog_pages, export_location]])
                    except Exception as e:
                        print(f"Failed to move space export [{space_key}] from [{temp_location}] to [{export_location}] : [{str(e)}]")
                        writer.writerows([[space_key, total_pages, blog_pages, str(e)]])
            else:
                # print(f"An error occurred in start_export for Space {space_key}: "
                #       f"{data if "message" not in data else data["message"]}")
                pass

        else:
            with open(csv_outputfile_path, mode='a', newline='') as file:
                writer = csv.writer(file)

                # Append new rows
                writer.writerows([[space_key, total_pages, blog_pages, f"ERROR CODE {res.status_code}"]])

            print(f"ERROR CODE {res.status_code} for {space_key}")

    except Exception as e:
        print(f"An error occurred in start_export for Space {space_key}: {str(e)}")


def start_export():
    if not os.path.exists(csv_outputfile_path):
        with open(csv_outputfile_path, mode='w', newline='') as file:
            writer = csv.writer(file)
            # Write the header
            writer.writerow(csv_header)

    with open("batch_input.txt", "r") as spaces:
        for space in spaces:
            export_space(space.rstrip())

def check_env_variable():
    print(f"CONFLUENCE_USER = {username}")
    print(f"CONFLUENCE_PWD = {password}")
    print(f"CONFLUENCE_BASE_URL = {domain}")
    print(f"CONFLUENCE_TEMP_DIR = {temp_dir}")
    print(f"CONFLUENCE_EXPORT_DIR = {export_dir}")

    if (username is None or username == ''
        or password is None or password == ''
        or domain is None or domain == ''
        or temp_dir is None or temp_dir == ''
        or export_dir is None or export_dir == ''):
        print("Ensure you have provided correct CONFLUENCE_USER, CONFLUENCE_PWD, CONFLUENCE_BASE_URL, CONFLUENCE_TEMP_DIR and CONFLUENCE_EXPORT_DIR")
        return False
    else:
        return True

if __name__ == "__main__":
    if check_env_variable():
        url = f"{domain}/rpc/json-rpc/confluenceservice-v2?os_authType=basic"
        start_export()
