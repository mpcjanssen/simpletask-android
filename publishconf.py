#!/usr/bin/env python
# -*- coding: utf-8 -*- #
from __future__ import unicode_literals

# This file is only used if you use `make publish` or
# explicitly specify it as your config file.

import os
import sys
sys.path.append(os.curdir)
from pelicanconf import *

SITEURL = '//mpcjanssen.nl/doc/simpletask'
RELATIVE_URLS = False
THEME = 'extras/online-theme'

DELETE_OUTPUT_DIRECTORY = True

SECTIONS = [('Blog', '../../index.html'),
        ('Tags', '../../tags.html'),
        ('Simpletask', ''),
        ('Tracker', '../../tracker/projects/simpletask-android')]
#       ('About', 'pages/about-me.html')]

# Feed generation is usually not desired when developing
FEED_ALL_ATOM = None
CATEGORY_FEED_ATOM = None
TRANSLATION_FEED_ATOM = None

TWITTER_USERNAME = 'MPCtje'
LINKEDIN_URL = 'https://www.linkedin.com/pub/mark-janssen/1/2a7/44'
GITHUB_URL = 'http://github.com/mpcjanssen'

MAIL_USERNAME = 'mpc.janssen'
MAIL_HOST = 'gmail.com'

