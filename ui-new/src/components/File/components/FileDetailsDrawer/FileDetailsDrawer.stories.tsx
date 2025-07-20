import React from 'react';
import type {Meta, StoryObj} from '@storybook/react';

import {FileDetailsDrawer} from './FileDetailsDrawer';

const meta: Meta<typeof FileDetailsDrawer> = {
  component: FileDetailsDrawer,
};

export default meta;

type Story = StoryObj<typeof FileDetailsDrawer>;

export const Basic: Story = {args: {}};
