#! /bin/env python3
import json
import qrencode
from PIL import Image
import sys
import hashlib
import base64
sha = hashlib.sha256()
sha.update(open(sys.argv[1],'rb').read())
checksum  = base64.urlsafe_b64encode(sha.digest()).decode().strip('=')
print(f"Check sum of {sys.argv[1]} is {checksum}, url is {sys.argv[2]}")

data = {
"android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": "org.mdeimonitorsview.android.recorder/.devowner.DevAdminReceiver",
"android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM": checksum,
"android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": sys.argv[2]
}

encoded_data = json.dumps(data).encode("utf-8")

qrencode.encode(encoded_data,qrencode.QR_ECLEVEL_H)[-1].save('qr.png')