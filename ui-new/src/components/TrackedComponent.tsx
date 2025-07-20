// TrackedComponent.tsx
import React from 'react';

type Props = {
  value: string;
};

const TrackedComponent: React.FC<Props> = React.memo(({ value }) => {
  return <div>Value: {value}</div>;
});

// Configuración de whyDidYouRender
TrackedComponent.whyDidYouRender = {
  logOnDifferentValues: true,
  trackHooks: true
};

export default TrackedComponent;
