#!/usr/bin/env python
# -*- coding: utf-8 -*- #
from __future__ import unicode_literals

AUTHOR = u'Mark Janssen'
SITENAME = u'Simpletask'
SITEURL = '.'

PAGE_DIR = ""
# PAGE_EXCLUDES = ('online',)

ARTICLE_DIR = "articles"
DELETE_OUTPUT_DIRECTORY = True
PAGE_URL = '{slug}.html'
PAGE_SAVE_AS = '{slug}.html'

AUTHORS_SAVE_AS = None
CATEGORIES_SAVE_AS = None
ARCHIVES_SAVE_AS = None
TAGS_SAVE_AS = None
DIRECT_TEMPLATES = ()


TIMEZONE = 'Europe/Paris'

DEFAULT_LANG = u'en'

# Feed generation is usually not desired when developing
FEED_ALL_ATOM = None
CATEGORY_FEED_ATOM = None
TRANSLATION_FEED_ATOM = None

PDF_GENERATOR = False
LOCALE = ""

STATIC_PATHS=['images']

THEME='pelican/mobile-theme'
