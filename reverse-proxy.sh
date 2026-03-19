#!/bin/bash

set -euxo pipefail

caddy run --watch --config ./Caddyfile --adapter caddyfile
